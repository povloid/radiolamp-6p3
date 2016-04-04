(ns ix.omut.component.tbody-trs-sel
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.tr-sel :as tr-sel]))

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
