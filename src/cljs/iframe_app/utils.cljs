(ns iframe-app.utils)

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

(defn click [element]
  (fire! element :click))

(defn string->int [str]
  (.parseInt js/window str 10))

(defn field-values-from-fields [fields]
  (flatten (map :possible-values fields)))



(= {:selections {:master-field {:name "0B27lu8gc2Iz", :id 5, :possible-values [{:name "TK3qM6aI98E55", :value "h2"} {:name "4zQDY2a4bbe91XOu", :value "U9"} {:name "eepJ83HZ", :value "26Rq"} {:name "024I", :value "fW1PJ9G"} {:name "sovZd", :value "ioh"} {:name "hsAi31ql9vD08IkU2Ed", :value "g885Ei0A715"} {:name "u351s25uJ01w7V92", :value "PXk7Jau9tr"} {:name "azwMGnfsZPv", :value "6b0Iv8mFV2d9wwo6f"} {:name "f063291TnmNhBA", :value "ROwA"}]}, :field-value {:name "azwMGnfsZPv", :value "6b0Iv8mFV2d9wwo6f"}, :slave-fields #{}}
                 :conditions #{}}
   {:conditions #{{:master-field {:name "0B27lu8gc2Iz", :id 5, :possible-values [{:name "TK3qM6aI98E55", :value "h2"} {:name "4zQDY2a4bbe91XOu", :value "U9"} {:name "eepJ83HZ", :value "26Rq"} {:name "024I", :value "fW1PJ9G"} {:name "sovZd", :value "ioh"} {:name "hsAi31ql9vD08IkU2Ed", :value "g885Ei0A715"} {:name "u351s25uJ01w7V92", :value "PXk7Jau9tr"} {:name "azwMGnfsZPv", :value "6b0Iv8mFV2d9wwo6f"} {:name "f063291TnmNhBA", :value "ROwA"}]}, :field-value {:name "hsAi31ql9vD08IkU2Ed", :value "g885Ei0A715"}, :slave-fields #{{:name "Q", :id 10, :possible-values [{:name "01hhNGg5CyYp9Q9", :value "SC4jk2W0Ly7CkkK2Qf2"} {:name "TGtpVrbK87", :value "O"} {:name "vtkW8AY85", :value "OU"} {:name "Xy0d2", :value "Ce7ZM66bc04FI6A50Rb"} {:name "8zZW1E0981y9Wi", :value "I6Qqds574Xbph68qx"} {:name "5m3Izvijskkq2x", :value "PNl1dPKd1SUzmZT96"} {:name "SCX45Hmj4I2CB", :value "m49J0K"} {:name "5fZwhe8Fe3vDjK68Zc5", :value "4J8V6C"} {:name "yYA", :value "hBSJ0O2nnheF7ghq"} {:name "3D445Z5YMyws", :value "wQ7q9p1x3M4Q7rv01"} {:name "q46bxqO0387N5", :value "aN9m0T8E6eIc7D8C2rN"} {:name "UZGST78OWSF65T0a3Wo", :value "fPbSb77U3F66OUoZPt"} {:name "b7u7y", :value "ZJ2vN277e0Q3Q00"}]}}}}
    :selections {:master-field {:name "0B27lu8gc2Iz", :id 5, :possible-values [{:name "TK3qM6aI98E55", :value "h2"} {:name "4zQDY2a4bbe91XOu", :value "U9"} {:name "eepJ83HZ", :value "26Rq"} {:name "024I", :value "fW1PJ9G"} {:name "sovZd", :value "ioh"} {:name "hsAi31ql9vD08IkU2Ed", :value "g885Ei0A715"} {:name "u351s25uJ01w7V92", :value "PXk7Jau9tr"} {:name "azwMGnfsZPv", :value "6b0Iv8mFV2d9wwo6f"} {:name "f063291TnmNhBA", :value "ROwA"}]}, :slave-fields #{}, :field-value {:name "azwMGnfsZPv", :value "6b0Iv8mFV2d9wwo6f"}}})