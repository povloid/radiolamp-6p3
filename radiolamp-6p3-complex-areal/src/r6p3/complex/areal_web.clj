;;**************************************************************************************************
;;* BEGIN Web layer for areal
;;* tag: <ref book web layer areal>
;;*
;;* description: Вебслой для Области, районы...
;;*
;;**************************************************************************************************

(ns r6p3.complex.areal-web
  (:require [clojure.java.io :as io]
            [r6p3.core :as c]
            [r6p3.complex.areal-core :as cc]
            [r6p3.transit :as transit]
            [r6p3.core :as c]))


(defn rest-areal-list [{{:keys [page page-size fts-query all]
                         :or {page 1 page-size 10 fts-query ""}} :params}
                       _]
  (-> cc/areal-select*

      (as-> query
          (if all
            query
            (c/com-pred-page* query (dec page) page-size)))

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (cc/areal-pred-search? query fts-query))))

      (korma.core/order :path_keynames)
      c/com-exec))

(defn rest-areal-find [{{id :id} :params} _]
  (-> (if id
        (c/com-find cc/areal id)
        {})))

(defn rest-areal-save [request {:keys [edit-role]}]
  (let [{:keys [row]} (request :params)
        row (cc/areal-save row)]
    row))

(defn rest-areal-delete [request {:keys [edit-role]}]
  (->> request
       :params
       :id
       (c/com-delete-for-id cc/areal)
       ((fn [_] {:result "OK"}))))
