(ns r6p3s.cpt.d3.core)



(def full-screen-width (- (-> js/window .-innerWidth) 40))



(def order-colors
  ["#0b0"
   "#0bb"
   "#bb0"
   "#b00"
   "#b0b"
   "#00b"
   "#333"
   ])


(defn min-max
  ([f-v f-min f-max l]
   (min-max f-v f-min 1 f-max 1 l))
  ([f-v f-min margin-k-min f-max margin-k-max l]
   (if (empty? l) #js [0 0]
       (let [[h & t]       l
             v             (f-v h)
             [min-v max-v] (reduce
                            (fn [[min-v max-v] row]
                              [(f-min min-v (f-v row)) (f-max max-v (f-v row))])
                            [v v] t)]
         (into-array [(* min-v margin-k-min) (* max-v margin-k-max)])))))
