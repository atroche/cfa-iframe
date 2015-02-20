(ns iframe-app.components.selections.user-type
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [iframe-app.utils :refer [active-conditions active-ticket-fields form->form-kw string->int]]
    [cljs.core.async :refer [put! chan <!]]
    [dommy.core :as dommy]))

(defcomponent user-type-selector [selections owner]
  (render-state [_ _]
    (html
      [:select {:name      "user-type"
                :on-change (fn [e]
                             (let [selector-channel (om/get-shared owner :selector-channel)
                                   new-user-type (keyword (dommy/value (.-target e)))]
                               (put! selector-channel
                                     {:selection-to-update :user-type
                                      :new-value           new-user-type})))}
       [:option {:value    "agent"
                 :selected (= :agent (:user-type selections))}
        "Agent"]
       [:option {:value    "end-user"
                 :selected (= :end-user (:user-type selections))}
        "End User"]])))