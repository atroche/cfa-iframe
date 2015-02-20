(ns iframe-app.components.footer
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [iframe-app.persistence :refer [persist-conditions! get-persisted-conditions]]
    [cljs.core.async :refer [put! chan <!]]))



(defn save-button-text [changed-conditions-count]
  (str "Save"
       (if (> changed-conditions-count 0)
         (str " (" changed-conditions-count ")"))))

(defn count-changed-conditions [conditions persisted-conditions]
  (let [first-diff (clojure.set/difference conditions
                                           persisted-conditions)]
    (if (empty? first-diff)
      (count (clojure.set/difference persisted-conditions
                                     conditions))
      (count first-diff))))

(defcomponent footer [app-state owner]
  (will-mount [_]
    (.addEventListener js/window
                       "storage"
                       (fn [e]
                         (om/refresh! owner))))
  (render-state [_ _]
    (html
      (let [{:keys [conditions selections]} app-state
            conditions-for-this-form (active-conditions selections @conditions)
            persisted-conditions-for-this-form (active-conditions selections (get-persisted-conditions))
            changed-conditions-count (and @conditions
                                          (count-changed-conditions conditions-for-this-form
                                                                    persisted-conditions-for-this-form))]

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
                                       :conditions
                                       (get-persisted-conditions))

                           (let [selector-channel (om/get-shared owner :selector-channel)]
                             (put! selector-channel
                                   {:selection-to-update :master-field
                                    :new-value           nil})))}
              "Cancel changes"]
             [:button.btn.btn-primary.save
              {:disabled (if-not conditions-changed? "disabled")
               :on-click (fn [e]
                           (when conditions-changed?
                             (persist-conditions! @conditions)
                             (om/refresh! owner)))}
              (save-button-text changed-conditions-count)]])]]))))
