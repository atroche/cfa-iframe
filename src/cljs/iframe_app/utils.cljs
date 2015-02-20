(ns iframe-app.utils)

(defn fire!
  "Creates an event of type `event-type`, optionally having
   `update-event!` mutate and return an updated event object,
   and fires it on `node`.
   Only works when `node` is in the DOM"
  [node event-type & [update-event!]]
  (let [update-event! (or update-event! identity)]
    (if (.-createEvent js/document)
      (let [event (.createEvent js/document "Event")]
        (.initEvent event (name event-type) true true)
        (.dispatchEvent node (update-event! event)))
      (.fireEvent node (str "on" (name event-type))
                  (update-event! (.createEventObject js/document))))))

(defn click [element]
  (fire! element :click))

(defn string->int [str]
  (.parseInt js/window str 10))

(defn form->form-kw [form]
  (keyword (str "form-" (:id form))))

(defn active-conditions [{:keys [ticket-form user-type] :as selections} conditions]
  (if conditions
    (let [form-kw (form->form-kw ticket-form)]
      (-> conditions user-type form-kw))))

(defn active-ticket-fields [selections ticket-forms]
  (let [form-id (get-in selections [:ticket-form :id])]
    (->> ticket-forms
         (filter #(= (:id %) form-id))
         first
         :ticket-fields
         (filter #(or (= :agent (:user-type selections))
                      (:show-to-end-user %))))))

(defn blank-conditions [ticket-forms]
  (into {}
        (for [user-type [:agent :end-user]]
          [user-type (into {}
                           (for [form ticket-forms]
                             [(form->form-kw form) #{}]))])))