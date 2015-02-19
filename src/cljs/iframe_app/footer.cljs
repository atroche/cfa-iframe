(ns iframe-app.footer
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.selectors :refer [slave-fields-selector value-selector
                                  master-field-selector user-type-selector
                                  ticket-form-selector]]
    [iframe-app.conditions-manager :refer [conditions-manager]]
    [iframe-app.selections-manager :refer [reset-irrelevant-selections update-conditions
                                           selections-manager]]
    [om-tools.dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [cljs.core.async :refer [put! chan <!]]))


(defn get-persisted-conditions []
  (cljs.reader/read-string (.getItem js/localStorage "conditions")))

(defn persist-conditions! [conditions]
  (.setItem js/localStorage "conditions" (pr-str conditions)))

(defn save-button-text [changed-conditions-count]
  (str "Save"
       (if (> changed-conditions-count 0)
         (str " (" changed-conditions-count ")"))))

(defn count-changed-conditions [conditions]
  (let [first-diff (clojure.set/difference conditions
                                           (get-persisted-conditions))]
    (if (empty? first-diff)
      (count (clojure.set/difference (get-persisted-conditions)
                                     conditions))
      (count first-diff))))

(defcomponent footer [app-state owner]
  (render-state [_ _]
    (html
      (let [{:keys [conditions selections]} app-state
            conditions (active-conditions selections conditions)
            changed-conditions-count (and conditions
                                          (count-changed-conditions conditions))]
        [:footer
         [:div.pane
          [:button.delete.text-error.deleteAll
           {:style {:display :none}}
           "Delete all conditional rules for this form"]
          (let [conditions-changed? (> changed-conditions-count 0)]
            [:div.action-buttons.pull-right
             [:button.btn.cancel
              {:disabled (if-not conditions-changed? "disabled")
               :on-click (fn [e]
                           (om/update! app-state
                                       [:conditions (:user-type selections) (form->form-kw (:ticket-form selections))]
                                       (get-persisted-conditions))

                           (let [selector-channel (om/get-shared owner :selector-channel)]
                             (put! selector-channel
                                   {:selection-to-update :master-field
                                    :new-value           nil}))

                           (om/refresh! owner))}
              "Cancel changes"]
             [:button.btn.btn-primary.save

              {:disabled (if-not conditions-changed? "disabled")
               :on-click (fn [e]
                           (when conditions-changed?
                             (persist-conditions! @conditions)
                             (om/refresh! owner)))}
              (save-button-text changed-conditions-count)]])]]))))