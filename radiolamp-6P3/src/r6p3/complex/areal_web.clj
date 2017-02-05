;;**************************************************************************************************
;;* BEGIN Web layer for areal
;;* tag: <ref book web layer areal>
;;*
;;* description: Вебслой для Области, районы...
;;*
;;**************************************************************************************************

(ns r6p3.complex.areal-web
  (:require [clojure.java.io :as io]
            [r6p3.core :as rc]
            [r6p3.transit :as transit]
            [r6p3.core :as c]))


(defn rest-areal-list [{{:keys [page page-size fts-query]
                         :or {page 1 page-size 10 fts-query ""}} :params}
                       _]
  (-> c/areal-select*
      (rc/com-pred-page* (dec page) page-size)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (c/areal-pred-search? query fts-query))))

      (korma.core/order :path_keynames)
      rc/com-exec))

(defn rest-areal-find [{{id :id} :params} _]
  (-> (if id
        (rc/com-find c/areal id)
        {})))

(defn rest-areal-save [request {:keys [edit-role]}]
  (println request edit-role)
  (rc/throw-when-no-role-from-request request edit-role)
  (let [{:keys [row]} (request :params)
        row (c/areal-save row)]
    row))

(defn rest-areal-delete [request {:keys [edit-role]}]
  (rc/throw-when-no-role-from-request request edit-role)
  (->> request
       :params
       :id
       (rc/com-delete-for-id c/areal)
       ((fn [_] {:result "OK"}))))
