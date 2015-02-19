(ns iframe-app.persistence
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
  (let [stored-conditions (.getItem js/localStorage "conditions")]
    (if (string? stored-conditions)
      (cljs.reader/read-string stored-conditions)
      stored-conditions)))

(defn persist-conditions! [conditions]
  (.setItem js/localStorage "conditions" (pr-str conditions)))