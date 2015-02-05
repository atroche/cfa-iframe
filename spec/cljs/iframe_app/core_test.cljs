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
            [iframe-app.condition-selector :refer [fields-without-field]]
            [iframe-app.core :refer [main app]]))

(enable-console-print!)

(defonce app-state (atom {:conditions #{}}))

(set! (.-warn js/console) (fn [t] nil))

; TODO: use test.check to generate fixtures
; TODO: make finders that wait til they find what they're looking for (like Capybara)

; do `reductions` over, producing maps of state

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

(def p println)

(defn get-master-field-element [field]
  (sel1 (str "a.master-field[data-id='" (:id field) "']")))

(defn get-field-value-element [field-value]
  (sel1 (str "a.field-value[data-value='" (:value field-value) "']")))

(defn get-slave-field-element [field]
  (sel1 (str "a.slave-field[data-id='" (:id field) "']")))

(defn element-click-fn [element-getter]
  (fn [arg]
    (click (element-getter arg))))

(def behaviour->action
  {:select-master-field {:element-getter (element-click-fn get-master-field-element)
                         :transform      (fn [state new-value]
                                           (assoc state :master-field new-value))}
   :select-field-value  {:element-getter (element-click-fn get-field-value-element)
                         :transform      (fn [state arg]
                                           (assoc state :field-value arg))}
   :select-slave-field  {:element-getter (element-click-fn get-slave-field-element)
                         :transform      (fn [fields arg]
                                           (update-in fields [:slave-fields] conj arg))}})



; what does it do to state? what should app-state be after a given action?
; for each action, there should be a function that takes app-state and
; the action (incl arg) and returns the new app-state

; e.g. select-master-field will change selections to reset the others and
;

(defn behave [[behaviour-name arg]]
  (when-let [action (:element-getter (behaviour->action behaviour-name))]
    (action arg)))

(defn selection-state-matches?
  "ensure that the expected state matches the app-state atom and the DOM"
  [state]

  (= (:selections @app-state)
     state))

(defn state-matches-dom? [selections]
  (let [master-field (:master-field selections)
        master-field-el (get-master-field-element master-field)
        actual-id (string->int (dommy/attr master-field-el :data-id))]
    (= actual-id (:id master-field))))

(defn get-states-for-behaviors [behaviours]
  (reductions (fn [state {:keys [transform-fn value]}]
                (transform-fn state value))
              {:master-field nil
               :field-value nil
               :slave-fields #{}}
              (map (fn [[behaviour-name value :as behaviour]]
                     {:transform-fn (:transform (behaviour->action behaviour-name))
                      :value        value})
                   behaviours)))

(deftest ^:async declarative
  (let [master-field (rand-nth dummy-ticket-fields)
        field-value (rand-nth (:possible-values master-field))
        slave-field (rand-nth (fields-without-field dummy-ticket-fields
                                                    master-field))
        behaviours [[:select-master-field master-field],
                    [:select-field-value field-value],
                    [:select-slave-field slave-field]]
        states (get-states-for-behaviors behaviours)]


    (go
      (doseq [[behaviour after-state] (map vector behaviours (rest states))]
        (behave behaviour)
        (wait-a-bit)
        (is (selection-state-matches? after-state))
        (is (state-matches-dom? after-state)))
      (wait-a-bit)

      (is (= "active" (dommy/class (dommy/parent (get-master-field-element (second (first behaviours)))))))
      (let [condition (first (:conditions @app-state))
            (is (and (= (:id master-field) (:id (:master-field condition)))
                     (= (:value field-value) (:value (:field-value condition)))
                     ((set (map :id (:slave-fields condition))) slave-field)))])
      (done))))



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
                  :shared {:pick-channel  (chan)
                           :ticket-fields dummy-ticket-fields}})
        (t/run-all-tests)
        (js/setTimeout
          (fn []
            (.callPhantom js/window "exit"))
          3000)))

;
;(deftest ^:async can-make-condition
;  (let [element->value-as-number #(-> %
;                                      (dommy/attr :value)
;                                      string->int)
;        master-field-el (first (sel "div.field-list a.field"))
;        master-field-id (element->value-as-number master-field-el)]
;    (go
;      (click master-field-el)
;      (println "just clicked master field")
;      (wait-a-bit)
;
;      (is (= "active" (dommy/class (dommy/parent master-field-el))))
;
;      (let [value-element (first (sel :a.value))
;            value-element-value (dommy/attr value-element :value)
;            _ (do (click value-element)
;                  (println "just clicked value element")
;                  (wait-a-bit)
;                  (is (= "active" (dommy/class (dommy/parent value-element)))))
;            slave-field-elements (take 2 (sel :.selectedField))
;
;            _ (do (is (not (empty? slave-field-elements)))
;                  (doseq [slave-field slave-field-elements]
;                    (click slave-field)
;                    (println "just clicked a slave field")
;                    (wait-a-bit))
;                  (wait-a-bit)
;                  (doseq [slave-field slave-field-elements]
;                    (is (.contains (dommy/class slave-field) "assigned"))))
;
;            _ (println (:conditions @app-state))
;
;            ; just to make sure it doesn't add new conditions
;            second-value-element (second (sel :a.value))
;            _ (do
;                (click second-value-element)
;                (wait-a-bit))
;
;
;            slave-field-element-ids (->> slave-field-elements
;                                         (map element->value-as-number)
;                                         set)
;
;            condition (first (:conditions @app-state))]
;        (is (and (= master-field-id (:id (:master-field condition)))
;                 (= value-element-value (:value (:field-value condition)))
;                 (= slave-field-element-ids (set (map :id (:slave-fields condition)))))))
;      (done))))
;
