(ns iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.condition-selector :refer [slave-fields-picker value-picker
                                           master-field-picker field-list]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [cljs.core.async :refer [put! chan <!]]))

; TODO: make values / slave fields within existing conditions highlight when viewed

(def dummy-ticket-fields
  [{:name            "Priority"
    :id              1234
    :type            :system
    :possible-values [{:name "Low" :value "low"}
                      {:name "Normal" :value "normal"}
                      {:name "High" :value "high"}
                      {:name "Urgent" :value "urgent"}]}

   {:name            "custom field"
    :id              4321
    :type            :tagger
    :possible-values [{:name "asdfasd" :value "asdfasd"}
                      {:name "12345" :value "12345"}]}
   {:name            "a third field"
    :id              9999
    :type            :tagger
    :possible-values [{:name "value number one" :value "v1"}
                      {:name "value number two" :value "v2"}]}])


;(def dummy-ticket-fields
;  [{:name "Sv04Aq7yY7", :id 17, :possible-values [{:name "OecayE7uM66yNg7", :value "7b5MgWO187H7"} {:name "hA6XZru", :value "P2SEC8uRyyH7"} {:name "e", :value "258Q4MO"} {:name "6pIz01R6r5j", :value "JZn9Yvb7o4"} {:name "96H9uIf85lb4p7a5Y", :value "b39Yge"} {:name "IIB88rw93OwZcjXi3JHC", :value "Uo89N478F4qKjjc"} {:name "IBA", :value "9FNOu"} {:name "n99S949uLX89fmsv", :value "0D6rLAq"} {:name "y6V6xncD6dCWqxBV", :value "5sj"} {:name "cvs52EtI9i5od", :value "wE5j60rg4N90549n8S"} {:name "rKMZPhBQ", :value "l8126KSc35m2PzX7PYPZx"} {:name "95x4", :value "s47FaLQ4"} {:name "mLVueS92Vw5", :value "w04N1k5kC6YQh828VA2"}]} {:name "72M4", :id 11, :possible-values [{:name "gFldhjp2csmW87ypv", :value "9p1HD"} {:name "74FY6", :value "s"} {:name "9Haqgp5JTn1", :value "ZK"} {:name "3rYKnIjF1JgKX", :value "GwPlqKe0i0zZRlgsYm4WN"} {:name "523", :value "64Bizx7ns5Yk5i6VK"} {:name "10k", :value "d39j95Z4b"} {:name "Y3IY", :value "g5C4g00ry2b2E2c7"} {:name "1110m9m", :value "M71"} {:name "E9gKZ", :value "G6DKOTV36Uf56s"} {:name "BfUzWk9dHBZ03K7vt43phd", :value "44j20MFp8vOx5oT"} {:name "fJQoaI9KC4GqnS", :value "1D15N808N"} {:name "UV6w2", :value "2kL5SfQ602Z2V6U7VC0"}]} {:name "AeDimJK8gwuWCR6RMn", :id 9, :possible-values [{:name "8Z4qBaL", :value "7f95T4V8JsuQ"} {:name "x7NhNdZ1up0AR9t", :value "55dc4lI2"} {:name "30Ct74", :value "02n0"} {:name "2e", :value "iieQM5"} {:name "70Erw", :value "v353B2r68c6kaDQHcA"} {:name "65HEiOUMz4w7sg5tLedc8", :value "067Aco"} {:name "Ep8bK0", :value "dKm8hdidhI"} {:name "7", :value "3LS0CW7"} {:name "63Z", :value "tu4QPX0a5"} {:name "4JZX38V6X", :value "7"} {:name "emjwuKE", :value "aLbUnwRzwnrU9Ce7"} {:name "deF1o559uh5FT2", :value "0vBMa25l7"} {:name "fY4gL0t5vm", :value "l3QOzh"} {:name "svjMl6GtH7l", :value "Qn15eb1d80wZ2"} {:name "mGL019o22t4YIn1507", :value "36nJ"} {:name "3aX4KfI540LW5", :value "xj"} {:name "1yPNk9O22", :value "9Rp0cO01188scQa"} {:name "2743", :value "H15kW962seT1ws8sX"}]} {:name "Apqv915jE06", :id 5, :possible-values [{:name "23MiQv6422gFUdIq0rI5M", :value "TbN3Tj"} {:name "t58", :value "aw8B3ZZt3GZ5IpV4AcF"} {:name "RNJW811869D", :value "0Ie55a"} {:name "3q6Sz92ek03VIbEN33", :value "VhQhXRK8r2P3cvRHOr602"} {:name "cariqP4p3Dj2HxVl", :value "0O7Ih8MUP"} {:name "6mi", :value "7137r3106k8"} {:name "Djg7eqgG2PHPADUz3c6pq", :value "52jMy7TcnkKJFCM"} {:name "qX111MTl84g8K8", :value "W3a"}]} {:name "m5w0B", :id 7, :possible-values [{:name "K40F256j", :value "7"} {:name "z1Bx7", :value "QU9nDu"} {:name "yDt4jUni14C", :value "9"} {:name "HK2g1G9u8h", :value "Lm535XhNMkfCSu59iM0"} {:name "I0w9zn4VSj5X5gXfWM", :value "8Ps5zlCxHBBrI"} {:name "exDTAS05TlQ9Ym", :value "sk24WF0v6"}]} {:name "e1hTU7m4cgz8rPlJAHg", :id 2, :possible-values [{:name "z191YW97KTK6ABoT5bXQ", :value "b"} {:name "v2wo57V0v4", :value "6sj2Vlj63M8179Xb8aNV"} {:name "6EL", :value "96949653aG4"}]}])


(defonce app-state (atom {:selections {:master-field nil
                                       :field-value  nil
                                       :slave-fields #{}}
                          :conditions #{}}))


(declare render-state)
(declare init-state)
(declare will-mount)


(defn condition-detail [{:keys [master-field field-value slave-fields]}]
  [:li.rule {:data-id (:id master-field)}
   [:div.ruleTitle
    [:i.icon-arrow-right]
    [:a.field
     (:name master-field)]]
   [:ul.unstyled
    (let [{:keys [name value]} field-value]

      [:li.value.selectedRule.hardSelect
       [:a.ruleItem {:value value}
        [:i.icon-arrow-right] name]
       [:div.pull-right
        [:a.deleteRule {:value value} "×"]]
       [:p
        [:i.icon-arrow-right]
        " "
        (let [slave-field-names (->> slave-fields
                                     (map :name)
                                     (clojure.string/join ", "))]
          [:em slave-field-names])]])]])



(defcomponent conditions-manager [conditions owner]
  (render-state [_ state]
    (html
      [:div.all_rules
       [:h4.rules_summary_title
        (str "Conditions in this form (" (count (:conditions app-state)) ")")]
       [:ul.unstyled.global
        (for [condition conditions]
          (condition-detail condition))]])))



(defn remove-condition
  "Takes a list of conditions, and returns all the ones that don't include
   the selected master field / value. Used when “updating” a condition to
   have new slave fields (actually removing old one and adding new one.)"
  [selected-master-field selected-field-value conditions]
  (set (remove (fn [{:keys [master-field field-value]}]
                 (and (= master-field selected-master-field)
                      (= field-value selected-field-value)))
               conditions)))

(defn update-conditions [conditions {:keys [master-field field-value slave-fields]}]
  (let [cleaned-conditions (remove-condition master-field
                                             field-value
                                             conditions)

        slave-fields-selected? (not (empty? slave-fields))
        new-condition {:master-field master-field
                       :field-value  field-value
                       :slave-fields slave-fields}]
    (if slave-fields-selected?
      (conj cleaned-conditions new-condition)
      cleaned-conditions)))


(defn reset-irrelevant-selections
  "When someone selects a master field (e.g.) we want to deselect the value
   and slave fields that were selected (because they only applied to that
   field. Likewise when someone selects a new value."
  [selections selection-to-update]
  (case selection-to-update
    :master-field (assoc selections :field-value nil
                                    :slave-fields #{})
    :field-value (assoc selections :slave-fields #{})
    selections))

(defcomponent app [app-state owner]
  (will-mount [_]
    (go-loop []
      ; handle updates from the three condition-selector components
      (let [pick-channel (om/get-shared owner :pick-channel)
            {:keys [selection-to-update new-value]} (<! pick-channel)
            new-selections (-> (:selections @app-state)
                               (assoc selection-to-update new-value)
                               (reset-irrelevant-selections selection-to-update))]
        (om/update! app-state :selections new-selections)

        (when (= selection-to-update :slave-fields)
          (let [updated-conditions (update-conditions (:conditions @app-state) new-selections)]
            (om/update! app-state [:conditions] updated-conditions))))
      (recur)))
  (render-state [_ {:keys [selected-field-id slave-fields-picker-chan
                           selected-value]}]
    (html
      [:section.ember-view.apps.app-554.apps_nav_bar.app_pane.main_panes
       [:header
        [:h3 "Conditional Fields"]]
       [:div.cfa_navbar
        {:data-main 1}
        [:div.pane.left
         [:h4 "Conditions for:"]
         [:select {:style {:width "90%"}}
          [:option "Agent"]
          [:option "what"]]

         [:h4 "Ticket Form:"]
         [:select {:style {:width "90%"}}
          [:option "Default Ticket Form"]
          [:option "what"]]

         [:aside.sidebar
          (om/build conditions-manager (:conditions app-state))]]
        [:div.pane.right.section
         [:section.main
          [:div.intro
           [:h3 "Manage conditional fields"]
           [:p
            "Configure your conditions to build your conditional fields. Select a field, a value for that field, and the appropriate field to show. "
            [:a
             {:target "_blank",
              :href
                      "https://support.zendesk.com/entries/26674953-Using-the-Conditional-Fields-app-Enterprise-Only-"}
             "Learn more."]]]

          [:ul.table-header.clearfix
           [:li "Fields"]
           [:li "Values"]
           [:li "Fields to show"]]

          [:div.table-wrapper
           [:table.table
            [:tbody
             [:tr
              (om/build master-field-picker
                        app-state)

              [:td.key
               [:div.values
                [:div.separator "Available"]
                (om/build value-picker
                          app-state)
                ]]
              [:td.selected
               [:div.values
                [:div.separator "Available"]
                (om/build slave-fields-picker (:selections app-state))]]]]]]]]

        [:footer
         [:div.pane
          [:div
           (prn-str (clj->js (:selections app-state)))]
          [:button.delete.text-error.deleteAll
           {:style {:display :none}}
           "Delete all conditional rules for this form"]
          [:div.action-buttons.pull-right
           [:button.btn.cancel {:disabled "disabled"} "Cancel changes"]
           [:button.btn.btn-primary.save {:disabled "disabled"} "Save"]]]]]])))





(defn main []
  (om/root
    app
    app-state
    {:target (. js/document (getElementById "app"))
     :shared {:pick-channel  (chan)
              :ticket-fields dummy-ticket-fields}}))


