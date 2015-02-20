(ns iframe-app.components.selections.slave-fields
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [iframe-app.utils :refer [active-conditions active-ticket-fields form->form-kw string->int]]
    [cljs.core.async :refer [put! chan <!]]))


(defn fields-without-field [fields field]
  (remove (partial = field)
          fields))



(defcomponent slave-fields-selector [{:keys [master-field field-value slave-fields] :as selections} owner]
  (render-state [_ _]
    (html
      [:ul
       (if field-value
         (let [ticket-forms (om/get-shared owner :ticket-forms)
               ticket-fields (active-ticket-fields selections ticket-forms)
               available-fields (fields-without-field ticket-fields
                                                      master-field)]
           (for [{:keys [name id] :as ticket-field} available-fields]
             [:li
              [:a
               (let [field-is-selected (slave-fields ticket-field)]
                 {:on-click (fn [e]
                              (let [{:keys [selector-channel]} (om/get-shared owner)
                                    updated-slave-fields (if field-is-selected
                                                           (disj slave-fields ticket-field)
                                                           (conj slave-fields ticket-field))]
                                (put! selector-channel
                                      {:selection-to-update :slave-fields
                                       :new-value           updated-slave-fields})))
                  :class    (str "selectedField slave-field " (if field-is-selected " assigned"))
                  :data-id  id
                  :style    {:font-weight (if field-is-selected "bold")}
                  :value    id})
               name]])))])))