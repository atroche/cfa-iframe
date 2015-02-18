(ns ^:figwheel-load iframe-app.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.selectors :refer [slave-fields-selector value-selector
                                  master-field-selector user-type-selector
                                  ticket-form-selector]]
    [iframe-app.conditions-manager :refer [conditions-manager]]
    [iframe-app.selections-manager :refer [reset-irrelevant-selections update-conditions
                                           selections-manager]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [ankha.core :as ankha]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [cljs.core.async :refer [put! chan <!]]))


(defonce app-state (atom {:selections {:master-field nil
                                       :field-value  nil
                                       :slave-fields #{}
                                       :user-type    :agent
                                       :ticket-form  nil}}))


(defcomponent app [app-state owner]
  (render-state [_ _]
    (let [{:keys [conditions selections]} app-state]
      (html
        [:section.ember-view.apps.app-554.apps_nav_bar.app_pane.main_panes
         [:header
          [:h3 "Conditional Fields"]]
         [:div.cfa_navbar
          {:data-main 1}
          [:div.pane.left
           [:h4 "Conditions for:"]
           (om/build user-type-selector selections)


           [:h4 "Ticket Form:"]
           (om/build ticket-form-selector selections)

           [:aside.sidebar
            (om/build conditions-manager (active-conditions selections conditions))]]

          (om/build selections-manager app-state)

          footer]]))))

(def footer
  [:footer
   [:div.pane
    [:button.delete.text-error.deleteAll
     {:style {:display :none}}
     "Delete all conditional rules for this form"]
    [:div.action-buttons.pull-right
     [:button.btn.cancel {:disabled "disabled"} "Cancel changes"]
     [:button.btn.btn-primary.save {:disabled "disabled"} "Save"]]]])

(defn main []
  (go
    (let [ticket-forms-chan (chan)]
      (fetch-ticket-forms ticket-forms-chan)

      (let [ticket-forms (<! ticket-forms-chan)]
        (swap! app-state assoc-in [:selections :ticket-form] (first ticket-forms))

        (om/root
          app
          app-state
          {:target (. js/document (getElementById "app"))
           :shared {:selector-channel (chan)
                    :ticket-forms     ticket-forms}})))))
