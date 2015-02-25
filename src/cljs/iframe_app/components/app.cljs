(ns iframe-app.components.app
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.components.sidebar :refer [sidebar]]
    [iframe-app.components.selections.manager :refer [selections-manager]]
    [iframe-app.components.footer :refer [footer]]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :refer-macros [html]]))


(defcomponent app [app-state owner]
  (render-state [_ _]
    (html
      [:section.ember-view.apps.app-554.apps_nav_bar.app_pane.main_panes
       [:div.cfa_navbar {:data-main 1}
        (om/build sidebar app-state)

        (om/build selections-manager app-state)

        (om/build footer app-state)
        ]])))
