;;**************************************************************************************************
;;* BEGIN Web layer for page
;;* tag: <ref book web layer page>
;;*
;;* description: Вебслой для страница
;;*
;;**************************************************************************************************

(ns r6p3.complex.page-web
  (:require [clojure.java.io :as io]
            [r6p3.transit :as transit]
            [korma.core :as kc]
            [r6p3.core :as c]))


(defn rest-page-list [{{:keys [page page-size fts-query]
                        :or {page 1 page-size 10 fts-query ""}} :params}]
  (-> c/page-select*
      (c/com-pred-page* (dec page) page-size)
      (kc/with c/content_type)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (c/page-pred-search? query fts-query))))

      (korma.core/order :id :desc)
      c/com-exec))

(defn rest-page-find [{{id :id} :params}]
  (-> (if id
        (-> c/page
            kc/select*
            (kc/where (= :id id))
            (kc/with c/content_type)
            c/com-exec-1)
        {})))

(defn rest-page-save [request]
  (let [{:keys [row]} (request :params)
        row (c/page-save row)]
    row))

(defn rest-page-delete [request]
  (->> request
       :params
       :id
       (c/com-delete-for-id c/page)
       ((fn [_] {:result "OK"}))))





(defn rest-page-images-list [request]
  (-> request
      :params
      (c/files_rel-select-files-by-* :page_id)
      c/file-pred-images*
      c/com-exec))

(defn rest-page-files-list [request]
  (-> request
      :params
      (c/files_rel-select-files-by-* :page_id)
      (c/file-pred-images* :not-image)
      c/com-exec))


(defn rest-page-files-rel-delete [{{:keys [id file-id]} :params}]
  (c/files_rel-delete :page_id {:id file-id} {:id id})
  {:result "OK"})



(defn rest-page-upload-image [request id]
  (c/web-file-upload
   (partial
    c/file-upload-rel-on-o c/page :page_id
    {:id (Long/parseLong id)}
    {:path-prefix "/image/" :ws [60 150 300]})
   (-> request :params :uploader))
  "OK")



(defn rest-page-upload-file [request id]
  (c/web-file-upload
   (partial
    c/file-upload-rel-on-o c/page :page_id
    {:id (Long/parseLong id)}
    {:path-prefix "/file/"})
   (-> request :params :uploader))
  "OK")
