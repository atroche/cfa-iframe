(ns iframe-app.components.footer
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]
    [iframe-app.utils :refer [active-conditions form->form-kw blank-conditions]]
    [iframe-app.persistence :refer [persist-conditions! get-persisted-conditions]]
    [cljs.core.async :refer [put! chan <!]]))



(defn save-button-text [changed-conditions-count]
  (str "Save"
       (if (> changed-conditions-count 0)
         (str " (" changed-conditions-count ")"))))

(defn count-changed-conditions [all-conditions selections]
  (if all-conditions
    (let [conditions (active-conditions selections all-conditions)
          persisted-conditions (active-conditions selections (get-persisted-conditions))
          first-diff (clojure.set/difference conditions
                                             persisted-conditions)]
      (if (empty? first-diff)
        (count (clojure.set/difference persisted-conditions
                                       conditions))
        (count first-diff)))
    0))

(defcomponent delete-conditions-button [app-state owner {:keys [save-channel]}]
  (render-state [_ _]
    (html
      [:button.delete.text-error.deleteAll
       {:on-click (fn [e]
                    (let [ticket-forms (om/get-shared owner :ticket-forms)
                          reset-conditions (blank-conditions ticket-forms)]
                      (om/update! app-state :conditions reset-conditions)
                      (put! save-channel reset-conditions)))}
       "Delete all conditional rules for this form"])))

(defcomponent cancel-button [app-state owner]
  (render-state [_ _]
    (let [{:keys [conditions selections]} app-state
          conditions-changed? (> (count-changed-conditions conditions selections) 0)]
      (html
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
        (println "rendering cancel")
        "Cancel changes"]))))

(defcomponent save-button [app-state owner {:keys [save-channel]}]
  (render-state [_ _]
    (let [{:keys [selections conditions]} app-state
          changed-conditions-count (count-changed-conditions conditions selections)]
      (html
        [:button.btn.btn-primary.save
         {:disabled (if-not (> changed-conditions-count 0) "disabled")
         :on-click (fn [_]
                      (put! save-channel conditions))}
         (save-button-text changed-conditions-count)]))))



(defcomponent footer [app-state owner]
  (will-mount [_]
    (.addEventListener js/window
                       "storage"
                       (fn [e]
                         (om/refresh! owner)))

    (let [save-channel (chan)]
      (om/set-state! owner :save-channel save-channel)
      (go-loop []
        (let [conditions (<! save-channel)]
          (persist-conditions! conditions)
          (om/update! app-state :saved true))
        (recur))))

  (render-state [_ {:keys [save-channel]}]
    (html
      [:footer
       [:div.pane
        (om/build delete-conditions-button app-state {:opts {:save-channel save-channel}})

        [:div.action-buttons.pull-right
         (om/build cancel-button app-state)

         (om/build save-button app-state {:opts {:save-channel save-channel}})]]])))
