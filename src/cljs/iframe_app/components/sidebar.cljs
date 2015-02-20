(ns iframe-app.components.sidebar
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]
    [iframe-app.components.conditions :refer [conditions-manager]]
    [iframe-app.components.selections.user-type :refer [user-type-selector]]
    [iframe-app.components.selections.ticket-form :refer [ticket-form-selector]]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [iframe-app.fetch-remote-data :refer [fetch-ticket-forms]]
    [iframe-app.persistence :refer [persist-conditions! get-persisted-conditions]]
    [cljs.core.async :refer [put! chan <!]]))

(defcomponent sidebar [app-state owner]
  (render-state [_ _]
    (let [{:keys [conditions selections]} app-state]
      (html
        [:div.pane.left
         (om/build user-type-selector selections)

         (om/build ticket-form-selector selections)

         [:aside.sidebar
          (om/build conditions-manager (active-conditions selections conditions))
          ]]))))