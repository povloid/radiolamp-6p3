(ns r6p3s.cpt.tbody-trs-sel
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.tr-sel :as tr-sel]))

(defn component [app owner {:keys [selection-type]
                            :or   {selection-type :one}
                            :as   opts}]
  (reify
    om/IRender
    (render [_]
      (apply
       dom/tbody nil
       (om/build-all
        tr-sel/component app
        {:opts
         (if (= selection-type :one)
           (assoc opts
                  :clear-selections-fn
                  (fn [_]
                    (om/transact!
                     app
                     (fn [data]
                       ;;(println (map omut-row-key data))
                       (vec (map #(c/omut-row-set-selected! % false) data))))))
           opts)})))))
