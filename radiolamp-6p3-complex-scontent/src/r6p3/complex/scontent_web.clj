;;**************************************************************************************************
;;* BEGIN Web layer for scontent
;;* tag: <ref book web layer scontent>
;;*
;;* description: Вебслой для контент для сайта
;;*
;;**************************************************************************************************

(ns r6p3.complex.scontent-web
  (:require [clojure.java.io :as io]
            [r6p3.core :as c]
            [r6p3.complex.scontent-core :as cc]
            [r6p3.transit :as transit]))



(defn rest-scontent-get-value [{{:keys [vtype keyname]} :params}]
  (cc/scontent-get-v vtype keyname))


(defn rest-scontent-list [{{:keys [page page-size fts-query]
                            :or {page 1 page-size 10 fts-query ""}} :params}]
  (-> cc/scontent-select*
      (c/com-pred-page* (dec page) page-size)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (cc/scontent-pred-search? query fts-query))))

      (korma.core/order :id :desc)
      c/com-exec))

(defn rest-scontent-find [{{id :id} :params}]
  (-> (if id
        (c/com-find cc/scontent id)
        {})))

(defn rest-scontent-save [request]
  (let [{:keys [row]} (request :params)
        row (c/com-save-for-id cc/scontent row)]
    row))

(defn rest-scontent-delete [request]
  (->> request
       :params
       :id
       (c/com-delete-for-id cc/scontent)
       ((fn [_] {:result "OK"}))))





(defn rest-scontent-images-list [request]
  (-> request
      :params
      (c/files_rel-select-files-by-* :scontent_id)
      c/file-pred-images*
      c/com-exec))

(defn rest-scontent-files-list [request]
  (-> request
      :params
      (c/files_rel-select-files-by-* :scontent_id)
      (c/file-pred-images* :not-image)
      c/com-exec))


(defn rest-scontent-files-rel-delete [{{:keys [id file-id]} :params}]
  (c/files_rel-delete :scontent_id {:id file-id} {:id id})
  {:result "OK"})



;; ----------------------------------------------------------------------------------


(defn rest-scontent-upload-image [request id]
  (c/web-file-upload
   (partial
    c/file-upload-rel-on-o cc/scontent :scontent_id
    {:id (Long/parseLong id)}
    {:path-prefix "/image/" :ws [60 150 300]})
   (-> request :params :uploader))
  "OK")



(defn rest-scontent-upload-file [request id]
  (c/web-file-upload
   (partial
    c/file-upload-rel-on-o cc/scontent :scontent_id
    {:id (Long/parseLong id)}
    {:path-prefix "/file/"})
   (-> request :params :uploader))
  "OK")
