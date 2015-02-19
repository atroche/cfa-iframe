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
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.footer :refer [footer]]
    [iframe-app.fetch-data :refer [fetch-ticket-forms]]
    [iframe-app.persistence :refer [get-persisted-conditions]]
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

          (om/build footer app-state)]]))))


(defn blank-conditions [ticket-forms]
  (into {}
        (for [user-type [:agent :end-user]]
          [user-type (into {}
                           (for [form ticket-forms]
                             [(form->form-kw form) #{}]))])))

(defn main []
  (go
    (let [ticket-forms-chan (chan)
          _ (fetch-ticket-forms ticket-forms-chan)
          ticket-forms (<! ticket-forms-chan)]
      (swap! app-state assoc-in [:selections :ticket-form] (first ticket-forms))

      (let [initial-conditions (if-let [persisted-conditions (get-persisted-conditions)]
                         persisted-conditions
                         (blank-conditions ticket-forms))]
        (swap! app-state assoc-in [:conditions] initial-conditions))




      ;(swap! app-state assoc-in [:conditions] (get-persisted-conditions))

      (om/root
        app
        app-state
        {:target (. js/document (getElementById "app"))
         :shared {:selector-channel (chan)
                  :ticket-forms     ticket-forms}}))))
