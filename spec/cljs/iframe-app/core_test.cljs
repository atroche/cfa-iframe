(ns core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var
                               done are)]
                   [cljs.core.async.macros :refer [go]])
  (:require [cemerick.cljs.test :as t]
            [om.core :as om :include-macros true]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >!]]
            [iframe-app.core :refer [main app]]))

(enable-console-print!)

(defn fire!
  "Creates an event of type `event-type`, optionally having
   `update-event!` mutate and return an updated event object,
   and fires it on `node`.
   Only works when `node` is in the DOM"
  [node event-type & [update-event!]]
  (let [update-event! (or update-event! identity)]
    (if (.-createEvent js/document)
      (let [event (.createEvent js/document "Event")]
        (.initEvent event (name event-type) true true)
        (.dispatchEvent node (update-event! event)))
      (.fireEvent node (str "on" (name event-type))
                  (update-event! (.createEventObject js/document))))))


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

(defn string->int [str]
  (.parseInt js/window str 10))

;(deftest top-level-div-is-there
;  (is (sel1 :.cfa_navbar)))

(deftest ^:async can-make-condition
  (let [wait-chan (chan)
        master-field (first (sel :a.field))
        master-field-id (dommy/attr master-field :value)
        wait-a-bit (fn [timeout] (js/setTimeout (fn []
                                           (put! wait-chan true))
                                         timeout))]
    (go
      (fire! master-field :click)
      (wait-a-bit 400)
      (<! wait-chan)
      (is (= "active" (dommy/class (dommy/parent master-field))))

      (let [value-element (first (sel :a.value))
            value-value (dommy/attr value-element :value)]
        (fire! value-element :click)
        (wait-a-bit 400)
        (<! wait-chan)
        (is (= "active" (dommy/class (dommy/parent value-element))))

        (let [slave-fields (take 2 (sel :.selectedField))]
          (is (not (empty? slave-fields)))
          (doseq [slave-field slave-fields]
            (fire! slave-field :click)
            (wait-a-bit 50)
            (<! wait-chan))

          (wait-a-bit 300)
          (<! wait-chan)
          (doseq [slave-field slave-fields]
            (is (.contains (dommy/class slave-field) "assigned")))

          (let [conditions (:conditions @app-state)]
            (is (= conditions
                   #{{:master-field-id (string->int master-field-id)
                      :value           value-value
                      :slave-fields    (set (map (fn [field]
                                                   (-> field
                                                       (dommy/attr :value)
                                                       string->int))
                                                 slave-fields))}})))))

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
