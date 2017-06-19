(ns r6p3.core-handler
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [ring.middleware.reload :refer (wrap-reload)] ;; reload temlates
            [ring.middleware.json :as json]
            [ring.middleware.session :as session]
            [ring.middleware.multipart-params :as multipart]

            [compojure.handler :as handler]
            [compojure.route :as route]

            [r6p3.core :as c]
            [r6p3.core-web :as cw]
            [r6p3.core-web-bootstrap :as cwb]

            [r6p3.transit :as transit]

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))


(defn parseLong [v] (Long/parseLong v))

(defn do-in-if-not-nil? [params path do-fn]
  (if (get-in params path) (do-fn params) params))

(defn update-in-if-not-nil? [params path f & args]
  (if-let [v (get-in params path)]
    (assoc-in params path (apply (partial f v) args))
    params))

;;**************************************************************************************************
;;* BEGIN
;;* tag: <>
;;*
;;* description:
;;*
;;**************************************************************************************************

(defn routes-ix* [cms-roles-set
                  {:keys [page-ixcms-main-params]
                   :or {page-ixcms-main-params {}}
                   :as spec}]

  (println "routes-ix* - spec>" spec)

  (routes
   (ANY "/c/set" request
        (cw/ix-accoc-parametr request))))

;; END
;;..................................................................................................


;;------------------------------------------------------------------------------
;; BEGIN: Fule uploading routes
;; tag: <file upload routes>
;; description: Маршруты для файлового аплоадера
;;------------------------------------------------------------------------------

(defn rest-files-list [{{:keys [page page-size fts-query]
                         :or {page 1 page-size 10 fts-query ""}} :params}]
  (-> c/files-select*
      (c/com-pred-page* (dec page) page-size)

      (as-> query
          (let [fts-query (clojure.string/trim fts-query)]
            (if (empty? fts-query)
              query
              (c/files-pred-search? query fts-query))))

      (korma.core/order :id :desc)
      c/com-exec))


(defn rest-file-find [request]
  (->> request
       :params
       :id
       (c/com-find c/files)))

(defn rest-file-edit [request]
  (-> request
      :params
      c/files-save))

(defn rest-file-delete [request]
  (-> request
      :params
      c/files-delete
      ((fn [_] {:result "OK"}))))


(defn routes-file* [edit-roles-set {:keys [save-file-fn-options]}]
  (routes


   ;; JSON
   (POST "/files/list" request
         (-> request
             rest-files-list
             ring.util.response/response))
   ;; TRANSIT
   (POST "/files/list/transit" request
         (-> request
             rest-files-list
             transit/response-transit))



   (multipart/wrap-multipart-params
    (POST "/files/upload" request
          (friend/authorize
           edit-roles-set
           (do
             (c/web-file-upload
              (fn [a b]
                (c/file-upload-o a b
                                        ;TODO: ДОбавить выбор варианта от типа контента
                                 (merge {:path-prefix "/image/"} save-file-fn-options)))
              (-> request :params :image-uploader))
             (ring.util.response/response "OK") ))))


   ;; JSON
   (POST "/files/find" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-find
              ring.util.response/response)))

   ;; TRANSIT
   (POST "/files/find/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-find
              transit/response-transit)))



   ;; JSON
   (POST "/files/edit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-edit
              ring.util.response/response)))

   ;; TRANSIT
   (POST "/files/edit/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-edit
              transit/response-transit)))


   ;; JSON
   (POST "/files/delete" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-delete
              ring.util.response/response
              cw/error-response-json)))

   ;; TRANSIT
   (POST "/files/delete/transit" request
         (friend/authorize
          edit-roles-set
          (-> request
              rest-file-delete
              transit/response-transit)))


   ;; Кэшированный источник файлов для картинок
   (GET "/image/*" {{path :*} :params :as request}
        (if (realized? c/files-root-directory)
          {:stawebuser 200
           :headers {"Cache-Control" (str "max-age=" (* 60 60 24 7)) }
           :body (clojure.java.io/file (str @c/files-root-directory "/" path))}
          (throw (Exception. "Значение пути в переменной files-root-directory еще не задано"))))

   ;; Источник файлов
   (GET "/file/*" {{path :*} :params :as request}
        (if (realized? c/files-root-directory)
          (clojure.java.io/file (str @c/files-root-directory "/" path))
          (throw (Exception. "Значение пути в переменной files-root-directory еще не задано"))))

   ))
