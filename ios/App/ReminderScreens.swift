import SwiftUI
import Contacts
import FakkerniCore

actor ContactsLoader {
    private let store = CNContactStore()
    static var canRead: Bool {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        if #available(iOS 18.0, *), status == .limited { return true }
        return status == .authorized
    }
    func load(askPermission: Bool = true) async throws -> [ContactPerson] {
        if !Self.canRead {
            guard askPermission, try await store.requestAccess(for: .contacts) else { throw AppError.permission }
        }
        let keys = [CNContactGivenNameKey, CNContactFamilyNameKey, CNContactOrganizationNameKey, CNContactPhoneNumbersKey] as [CNKeyDescriptor]
        let request = CNContactFetchRequest(keysToFetch: keys); request.sortOrder = .userDefault
        var people: [ContactPerson] = []
        try store.enumerateContacts(with: request) { contact, _ in
            let name = [contact.givenName, contact.familyName].filter { !$0.isEmpty }.joined(separator: " ")
            for (index, number) in contact.phoneNumbers.enumerated() {
                let person = ContactPerson(id: contact.identifier + "-\(index)", name: name.isEmpty ? contact.organizationName : name, phone: number.value.stringValue)
                if !person.phone.isEmpty { people.append(person) }
            }
        }
        return people
    }
}

struct ContactPickerScreen: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var query: String
    @State private var contacts: [ContactPerson] = []
    @State private var loading = true
    @State private var failed = false
    var choose: (ContactPerson) -> Void
    init(query: String = "", choose: @escaping (ContactPerson) -> Void) { _query = State(initialValue: query); self.choose = choose }
    private var results: [ContactPerson] { ContactMatcher.search(query, in: contacts) }
    var body: some View {
        NavigationStack {
            List {
                if loading { ProgressView(model.t("نحمّل الأسماء…", "Loading contacts…")) }
                else if failed {
                    Text(model.t("اسمح لفكّرني بالوصول للأسماء من إعدادات الآيفون. الأسماء تبقى على جهازك.", "Allow Fakkerni to access contacts in iPhone Settings. Contacts stay on your device."))
                    Button(model.t("فتح الإعدادات", "Open Settings")) { UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!) }
                } else if results.isEmpty { Text(model.t("ما لقيناش اسم أو رقم مطابق.", "No matching name or number.")) }
                ForEach(results) { person in
                    Button { choose(person); dismiss() } label: {
                        VStack(alignment: .leading, spacing: 4) { Text(person.name.isEmpty ? person.phone : person.name).foregroundStyle(.primary); Text(person.phone).font(.cairo(14, relativeTo: .subheadline)).foregroundStyle(Palette.secondary).environment(\.layoutDirection, .leftToRight) }.frame(minHeight: 48)
                    }
                }
            }.searchable(text: $query, prompt: model.t("اسم عربي، English، أو رقم", "Arabic / English name or number"))
                .navigationTitle(model.t("اختار شخص", "Choose a person")).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(model.t("إلغاء", "Cancel")) { dismiss() } } }
                .task { do { contacts = try await ContactsLoader().load(); loading = false } catch { failed = true; loading = false } }
        }
    }
}

struct ReminderEditor: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let draft: VoiceDraft?
    let existing: Reminder?
    @State private var title: String
    @State private var eventTime: Date
    @State private var hasTime: Bool
    @State private var phone: String
    @State private var contactName: String
    @State private var saving = false
    @State private var error: String?
    @State private var contacts = false
    @State private var matchCount = 0
    init(draft: VoiceDraft? = nil, existing: Reminder? = nil) {
        self.draft = draft; self.existing = existing
        _title = State(initialValue: existing?.title ?? draft?.title ?? "")
        let suggested = existing?.eventTime ?? draft?.eventDate()
        _eventTime = State(initialValue: suggested ?? Date().addingTimeInterval(7200))
        _hasTime = State(initialValue: suggested != nil)
        _phone = State(initialValue: existing?.phone ?? draft?.phone ?? "")
        _contactName = State(initialValue: existing?.contactName ?? draft?.contactName ?? "")
    }
    var body: some View {
        NavigationStack {
            Form {
                if let draft, !draft.transcript.isEmpty {
                    Section(model.t("كلامك", "Your words")) { Text(draft.transcript).textSelection(.enabled).foregroundStyle(Palette.secondary) }
                    if draft.extractionFailed { Section { Text(model.t("كتبنا كلامك، لكن تحليل الموعد ما كملش. اختار التاريخ والساعة بيدك.", "Your words were transcribed, but the time could not be extracted. Choose the date and time below.")) } }
                    else if draft.needsReview { Section { Text(model.t("راجع التفاصيل، وكمّل أي موعد أو اسم ناقص.", "Review the details and fill in any missing time or person.")) } }
                }
                Section(model.t("شن عندك؟", "What do you need to do?")) {
                    TextField(model.t("اكتب التذكير", "Write your reminder"), text: $title, axis: .vertical).lineLimit(2...5).accessibilityIdentifier("reminder-title")
                        .onChange(of: title) { title = Digits.latin($0) }
                }.listRowBackground(Palette.surface)
                Section(model.t("إمتى؟", "When?")) {
                    HStack(spacing: 12) {
                        Button(model.t("اليوم", "Today")) { chooseDay(0) }.accessibilityIdentifier("day-today")
                        Button(model.t("غدوة", "Tomorrow")) { chooseDay(1) }.accessibilityIdentifier("day-tomorrow")
                    }.buttonStyle(.bordered).controlSize(.large).frame(minHeight: 44)
                    DatePicker(model.t("التاريخ", "Date"), selection: $eventTime, in: Calendar.current.startOfDay(for: Date())..., displayedComponents: .date).environment(\.locale, Locale(identifier: "en_GB")).accessibilityIdentifier("event-date")
                    DatePicker(model.t("الساعة", "Time"), selection: $eventTime, displayedComponents: .hourAndMinute).environment(\.locale, Locale(identifier: "en_GB")).accessibilityIdentifier("event-time")
                    if !hasTime {
                        Text(model.t("اختار اليوم أو غدوة، أو غيّر التاريخ والساعة. الموعد الظاهر اقتراح فقط.", "Choose Today or Tomorrow, or change the date and time. The displayed time is only a suggestion.")).font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary)
                    } else if eventTime <= Date() {
                        Text(model.t("الوقت هذا فات. اختار ساعة جاية.", "That time has passed. Choose a future time.")).font(.cairo(13, relativeTo: .caption)).foregroundStyle(.red)
                    } else {
                        Text(eventTime.timeIntervalSinceNow > 3600 ? model.t("الإشعار يجيك قبل الموعد بساعة.", "You will be notified one hour before the event.") : model.t("الموعد قريب: الإشعار يجيك بعد الحفظ مباشرة.", "The event is soon: the notification arrives shortly after saving."))
                            .font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary)
                    }
                }.listRowBackground(Palette.surface).onChange(of: eventTime) { _ in hasTime = true }
                Section(model.t("شخص نتصل به — اختياري", "Someone to call — optional")) {
                    Button { contacts = true } label: { HStack { MorphIcon(name: "phone").frame(width: 22, height: 22); Text(contactName.isEmpty ? model.t("نبحث في الأسماء والأرقام", "Search names and numbers") : contactName); Spacer(); Image(systemName: "chevron.forward").font(.caption) }.frame(minHeight: 44) }.accessibilityIdentifier("choose-contact")
                    if matchCount > 1 && phone.isEmpty { Text(model.t("في أكثر من اسم مطابق. اختار الشخص الصحيح.", "More than one contact matches. Choose the right person.")).font(.cairo(13, relativeTo: .caption)).foregroundStyle(Palette.secondary) }
                    TextField(model.t("رقم الهاتف", "Phone number"), text: $phone).keyboardType(.phonePad).environment(\.layoutDirection, .leftToRight).accessibilityIdentifier("phone-number")
                        .onChange(of: phone) { phone = Digits.latin($0) }
                    if !contactName.isEmpty || !phone.isEmpty { Button(model.t("إزالة الشخص", "Remove person"), role: .destructive) { phone = ""; contactName = "" } }
                }.listRowBackground(Palette.surface)
                if let error { Section { Text(error).foregroundStyle(.red); Button(model.t("إعدادات الآيفون", "iPhone Settings")) { UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!) } } }
            }.scrollContentBackground(.hidden).background(Palette.background)
                .navigationTitle(model.t(existing == nil ? "راجع التذكير" : "تعديل التذكير", existing == nil ? "Review reminder" : "Edit reminder")).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button(model.t("إلغاء", "Cancel")) { dismiss() }.disabled(saving).accessibilityIdentifier("cancel-editor") } }
                .safeAreaInset(edge: .bottom) {
                    Button { save() } label: { if saving { ProgressView().tint(Palette.inverse) } else { Text(model.t("موافق، احفظ التذكير", "Approve & save reminder")) } }
                        .buttonStyle(PrimaryButtonStyle()).disabled(saving || !hasTime || eventTime <= Date() || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .accessibilityIdentifier("approve-reminder").padding(20).background(Palette.background)
                }
                .sheet(isPresented: $contacts) { ContactPickerScreen(query: contactName) { person in contactName = person.name; phone = person.phone } }
                .interactiveDismissDisabled(saving)
                .task {
                    guard existing == nil, phone.isEmpty, !contactName.isEmpty, ContactsLoader.canRead,
                          let all = try? await ContactsLoader().load(askPermission: false) else { return }
                    let matches = all.filter { ContactMatcher.nameMatches(contactName, $0.name) }
                    matchCount = matches.count
                    if matches.count == 1 { phone = matches[0].phone; contactName = matches[0].name }
                }
        }
    }
    private func chooseDay(_ offset: Int) {
        let calendar = Calendar.current
        let day = calendar.date(byAdding: .day, value: offset, to: Date()) ?? Date()
        let time = calendar.dateComponents([.hour, .minute], from: eventTime)
        eventTime = calendar.date(bySettingHour: time.hour ?? 12, minute: time.minute ?? 0, second: 0, of: day) ?? day
        hasTime = true
    }
    private func save() {
        let cleanedPhone = PhoneNumber.clean(phone)
        guard phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !cleanedPhone.isEmpty else { error = model.t("رقم الهاتف غير صحيح.", "The phone number is invalid."); return }
        saving = true; error = nil
        let reminder = Reminder(id: existing?.id ?? UUID(), title: title, eventTime: eventTime, phone: cleanedPhone, contactName: contactName)
        Task {
            do { try await model.save(reminder); saving = false; UINotificationFeedbackGenerator().notificationOccurred(.success); dismiss() }
            catch { saving = false; self.error = model.message(for: error) }
        }
    }
}

struct ReminderListScreen: View {
    @EnvironmentObject private var model: AppModel
    @State private var editing: Reminder?
    @State private var adding = false
    @State private var selected = "upcoming"
    private var entries: [Reminder] {
        if selected == "upcoming" { return model.upcoming }
        return model.reminders.filter { $0.completed || $0.eventTime <= Date() }.sorted { $0.eventTime > $1.eventTime }
    }
    var body: some View {
        List {
            Picker(model.t("التذكيرات", "Reminders"), selection: $selected) { Text(model.t("القادمة", "Upcoming")).tag("upcoming"); Text(model.t("السابقة", "Past")).tag("past") }.pickerStyle(.segmented).listRowBackground(Color.clear)
            if entries.isEmpty {
                VStack(spacing: 14) { MorphIcon(name: "bell").frame(width: 44, height: 44); Text(model.t("ما عندكش تذكيرات هنا.", "No reminders here yet.")); Text(model.t("قول شن عندك، أو اضغط + واكتبه.", "Say what you need to do, or tap + to type it.")).font(.cairo(14, relativeTo: .subheadline)).foregroundStyle(Palette.secondary) }.frame(maxWidth: .infinity).padding(.vertical, 36).listRowBackground(Color.clear)
            }
            ForEach(entries) { reminder in
                Button { editing = reminder } label: {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .top, spacing: 12) {
                            MorphIcon(name: reminder.completed ? "check" : reminder.phone.isEmpty ? "clock" : "phone").frame(width: 24, height: 24).padding(10).background(Palette.background, in: Circle())
                            VStack(alignment: .leading, spacing: 6) { Text(reminder.title).font(.cairo(18, relativeTo: .headline).weight(.semibold)).foregroundStyle(.primary); if !reminder.contactName.isEmpty { Text(reminder.contactName).font(.cairo(14, relativeTo: .subheadline)).foregroundStyle(Palette.secondary) } }
                        }
                        Divider()
                        HStack { Text(formatted(reminder.eventTime)); Spacer(); Text(reminder.completed ? model.t("تم", "Done") : model.t("قبلها بساعة", "1 hour before")) }.font(.cairo(12, relativeTo: .caption)).foregroundStyle(Palette.secondary)
                    }.padding(.vertical, 12)
                }.listRowBackground(Palette.surface)
                    .swipeActions(edge: .trailing) { Button(role: .destructive) { model.delete(reminder) } label: { Label(model.t("حذف", "Delete"), systemImage: "trash") } }
                    .swipeActions(edge: .leading) { if !reminder.completed { Button { model.complete(reminder) } label: { Label(model.t("تم", "Done"), systemImage: "checkmark") }.tint(.green) } }
                    .contextMenu {
                        if !reminder.phone.isEmpty { Button { if let url = URL(string: "tel:"+reminder.phone) { UIApplication.shared.open(url) } } label: { Label(model.t("اتصال", "Call"), systemImage: "phone") } }
                        Button { editing = reminder } label: { Label(model.t("تعديل", "Edit"), systemImage: "pencil") }
                    }
            }
        }.listStyle(.insetGrouped).scrollContentBackground(.hidden).background(Palette.background)
            .navigationTitle(model.t("تذكيراتي", "My reminders"))
            .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button { adding = true } label: { MorphIcon(name: "plus").frame(width: 24, height: 24).frame(width: 44, height: 44) }.accessibilityLabel(model.t("تذكير جديد", "New reminder")).accessibilityIdentifier("add-reminder") } }
            .sheet(item: $editing) { ReminderEditor(existing: $0) }
            .sheet(isPresented: $adding) { ReminderEditor() }
    }
    private func formatted(_ date: Date) -> String {
        let formatter = DateFormatter(); formatter.locale = Locale(identifier: model.ar ? "ar_LY@numbers=latn" : "en_GB"); formatter.dateFormat = "EEE d MMM · HH:mm"
        return Digits.latin(formatter.string(from: date))
    }
}
