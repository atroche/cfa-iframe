(ns core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var
                               done are)]
                   [iframe-app.macros :refer [wait-a-bit]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >!]]
            [iframe-app.utils :refer [fire! click string->int]]
            [iframe-app.core :refer [main app]]))

(enable-console-print!)

; TODO: use test.check to generate fixtures
; TODO: make finders that wait til they find what they're looking for (like Capybara)

(def dummy-ticket-fields
  [{:name            "Priority"
    :id              1234
    :type            :system
    :possible-values [{:name "Low" :value "low"}
                      {:name "Normal" :value "normal"}
                      {:name "High" :value "high"}
                      {:name "Urgent" :value "urgent"}]}

   {:name            "custom field"
    :id              4321
    :type            :tagger
    :possible-values [{:name "asdfasd" :value "asdfasd"}
                      {:name "12345" :value "12345"}]}
   {:name            "a third field"
    :id              9999
    :type            :tagger
    :possible-values [{:name "value number one" :value "v1"}
                      {:name "value number two" :value "v2"}]}])

(defonce app-state (atom {:ticket-fields dummy-ticket-fields
                          :conditions    #{}}))

(set! (.-onload js/window)
      (fn []
        (.appendChild (.-body js/document)
                      (let [element (.createElement js/document "div")
                            _ (set! (.-id element) "app")]
                        element))
        ; instantiate app component with dummy data
        ; click on some of the things
        ; test some stuff =)
        (om/root app
                 app-state
                 {:target (. js/document (getElementById "app"))
                  :shared {:pick-channel (chan)}})
        (t/run-all-tests)
        (js/setTimeout
          (fn []
            (.callPhantom js/window "exit"))
          3000)))


(deftest ^:async can-make-condition
  (let [element->value-as-number #(-> %
                                      (dommy/attr :value)
                                      string->int)
        master-field (first (sel :a.field))
        master-field-id (element->value-as-number master-field)]
    (go
      (click master-field)
      (wait-a-bit)

      (is (= "active" (dommy/class (dommy/parent master-field))))

      (let [value-element (first (sel :a.value))
            value-value (dommy/attr value-element :value)
            _ (do (click value-element)
                  (wait-a-bit)
                  (is (= "active" (dommy/class (dommy/parent value-element)))))
            slave-fields (take 2 (sel :.selectedField))
            slave-fields-values (->> slave-fields
                                     (map element->value-as-number)
                                     set)
            _ (do (is (not (empty? slave-fields)))
                  (doseq [slave-field slave-fields]
                    (click slave-field)
                    (wait-a-bit))
                  (wait-a-bit)
                  (doseq [slave-field slave-fields]
                    (is (.contains (dommy/class slave-field) "assigned"))))

            conditions (:conditions @app-state)
            expected-conditions #{{:master-field-id master-field-id
                                   :value           value-value
                                   :slave-fields    slave-fields-values}}
            _ (do (is (= conditions expected-conditions)))])
      (done))))

;(click master-field)
;(is (active? master-field))
;(click value-field)
;(is (active? value-field))
;(click slave-field)
;(is (active? value-field))
;(is (solitary (:conditions app-state)))
;(is (= {:etc 1} (first (:conditions app-state))))
;
;; poll element or do timeout-0?
