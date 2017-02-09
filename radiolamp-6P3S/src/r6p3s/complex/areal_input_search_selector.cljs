(ns r6p3s.complex.areal-input-search-selector
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.cpt.input-search-selector :as input-search-selector]
            [r6p3s.net :as rnet]
            [r6p3s.core :as rc]))


(def app-init input-search-selector/app-init)

(def selected input-search-selector/selected)
(def selected-set input-search-selector/selected-set)


(defn make-opts-1 [{:keys [uri]}]
  {:search-data-fn
   (fn [chan-ret-data text]
     (rnet/get-data
      uri
      {:fts-query text
       :page-size 10}
      (fn [response]
        (->> response
             vec
             (put! chan-ret-data)))))
   :ui-row-fn
   (fn [{:keys [path_keynames keyname]}]
     [(dom/td nil
              (dom/div #js {:className "text-primary"} keyname)
              (dom/small #js {:className "text-info"} path_keynames))])})


(def component
  (fn [app own opts]
    (input-search-selector/component
     app own (merge (make-opts-1 opts) opts))))


(def component-form-group
  (fn [app own opts]
    (input-search-selector/component-form-group
     app own (merge {:spec-input (make-opts-1 opts)} opts))))
