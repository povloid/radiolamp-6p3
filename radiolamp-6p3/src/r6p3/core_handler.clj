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





;;**************************************************************************************************
;;* BEGIN Users reference book
;;* tag: <webusers ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************



(defn rest-webusers-list [{{:keys [page page-size fts-query]
                            :or {page 1 page-size 10 fts-query ""}} :params
                           :as request}

                          {:keys [webusers-list-pred-fn view-role edit-role]}]

  (if (c/is-allow-from-request request #{edit-role view-role})
    (-> c/webuser-select*
        (c/com-pred-page* (dec page) page-size)

        (as-> query
            (let [fts-query (clojure.string/trim fts-query)]
              (if (empty? fts-query)
                query
                (c/webuser-pred-search? query fts-query))))


        (as-> query
            (if webusers-list-pred-fn
              (webusers-list-pred-fn query request)
              query))

        (korma.core/order :id :desc)
        c/com-exec)
    []))

(defn rest-webusers-find [{{id :id} :params :as request} {:keys [view-role]}]

  (c/throw-when-no-role-from-request request view-role)

  (let [webroles (c/webrole-list true)]
    (-> (if id
          (-> id
              ((partial c/com-find c/webuser))
              (dissoc :password)
              (as-> row
                  (assoc row :troles-set
                         [webroles (c/webuserwebrole-own-get-rels-set row)])))
          (-> {} ;; new
              (assoc :troles-set
                     [webroles #{}]))))))

(defn rest-webusers-save [request {:keys [edit-role]}]

  (c/throw-when-no-role-from-request request edit-role)

  (let [{:keys [row user-roles-keys-set]} (request :params)
        row (-> row
                (as-> row
                    (if (empty? (:password row)) (dissoc row :password)
                        (update-in row [:password] creds/hash-bcrypt)))
                ((partial c/com-save-for-id c/webuser)))]

    (when user-roles-keys-set
      (println (map keyword user-roles-keys-set))
      (c/webuserwebrole-add-rels-for-keynames row (map keyword user-roles-keys-set)))
    row))

(defn rest-webusers-delete [request {:keys [edit-role]}]

  (c/throw-when-no-role-from-request request edit-role)

  (-> request
      :params
      :id
      ((partial c/com-delete-for-id c/webuser))
      ((fn [_] {:result "OK"}))))

(defn rest-webusers-change-password [request]
  (-> request
      :params
      (update-in [:password] creds/hash-bcrypt)
      (assoc :username (-> request :session :cemerick.friend/identity :current))
      c/webuser-save-for-username))


(defn routes-webusers* [{:keys [view-role
                                edit-role]
                         :as opts}]
  (routes
   (context "/tc/rb/webusers" []


            ;; JSON
            (POST "/list" request
                  (-> request
                      (rest-webusers-list opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/list/transit" request
                  (-> request
                      (rest-webusers-list opts)
                      transit/response-transit))



            ;; JSON
            (POST "/find" request
                  (-> request
                      (rest-webusers-find opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/find/transit" request
                  (-> request
                      (rest-webusers-find opts)
                      transit/response-transit))



            ;; JSON
            (POST "/save" request
                  (-> request
                      (rest-webusers-save opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/save/transit" request
                  (-> request
                      (rest-webusers-save opts)
                      transit/response-transit))



            ;; JSON
            (POST "/delete" request
                  (-> request
                      (rest-webusers-delete opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/delete/transit" request
                  (-> request
                      (rest-webusers-delete opts)
                      transit/response-transit))



            ;; JSON
            (POST "/change-password" request
                  (-> request
                      rest-webusers-change-password
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/change-password/transit" request
                  (-> request
                      rest-webusers-change-password
                      transit/response-transit))


            )))

;; END Users reference book
;;..................................................................................................
;;**************************************************************************************************
;;* BEGIN Users reference book
;;* tag: <webusers ref book>
;;*
;;* description: Справочник управления пользователями
;;*
;;**************************************************************************************************



(defn rest-webusers-list [{{:keys [page page-size fts-query]
                            :or {page 1 page-size 10 fts-query ""}} :params
                           :as request}

                          {:keys [webusers-list-pred-fn view-role edit-role]}]

  (if (c/is-allow-from-request request #{edit-role view-role})
    (-> c/webuser-select*
        (c/com-pred-page* (dec page) page-size)

        (as-> query
            (let [fts-query (clojure.string/trim fts-query)]
              (if (empty? fts-query)
                query
                (c/webuser-pred-search? query fts-query))))


        (as-> query
            (if webusers-list-pred-fn
              (webusers-list-pred-fn query request)
              query))

        (korma.core/order :id :desc)
        c/com-exec)
    []))

(defn rest-webusers-find [{{id :id} :params :as request} {:keys [view-role]}]

  (c/throw-when-no-role-from-request request view-role)

  (let [webroles (c/webrole-list true)]
    (-> (if id
          (-> id
              ((partial c/com-find c/webuser))
              (dissoc :password)
              (as-> row
                  (assoc row :troles-set
                         [webroles (c/webuserwebrole-own-get-rels-set row)])))
          (-> {} ;; new
              (assoc :troles-set
                     [webroles #{}]))))))

(defn rest-webusers-save [request {:keys [edit-role]}]

  (c/throw-when-no-role-from-request request edit-role)

  (let [{:keys [row user-roles-keys-set]} (request :params)
        row (-> row
                (as-> row
                    (if (empty? (:password row)) (dissoc row :password)
                        (update-in row [:password] creds/hash-bcrypt)))
                ((partial c/com-save-for-id c/webuser)))]

    (when user-roles-keys-set
      (println (map keyword user-roles-keys-set))
      (c/webuserwebrole-add-rels-for-keynames row (map keyword user-roles-keys-set)))
    row))

(defn rest-webusers-delete [request {:keys [edit-role]}]

  (c/throw-when-no-role-from-request request edit-role)

  (-> request
      :params
      :id
      ((partial c/com-delete-for-id c/webuser))
      ((fn [_] {:result "OK"}))))

(defn rest-webusers-change-password [request]
  (-> request
      :params
      (update-in [:password] creds/hash-bcrypt)
      (assoc :username (-> request :session :cemerick.friend/identity :current))
      c/webuser-save-for-username))


(defn routes-webusers* [{:keys [view-role
                                edit-role]
                         :as opts}]
  (routes
   (context "/tc/rb/webusers" []


            ;; JSON
            (POST "/list" request
                  (-> request
                      (rest-webusers-list opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/list/transit" request
                  (-> request
                      (rest-webusers-list opts)
                      transit/response-transit))



            ;; JSON
            (POST "/find" request
                  (-> request
                      (rest-webusers-find opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/find/transit" request
                  (-> request
                      (rest-webusers-find opts)
                      transit/response-transit))



            ;; JSON
            (POST "/save" request
                  (-> request
                      (rest-webusers-save opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/save/transit" request
                  (-> request
                      (rest-webusers-save opts)
                      transit/response-transit))



            ;; JSON
            (POST "/delete" request
                  (-> request
                      (rest-webusers-delete opts)
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/delete/transit" request
                  (-> request
                      (rest-webusers-delete opts)
                      transit/response-transit))



            ;; JSON
            (POST "/change-password" request
                  (-> request
                      rest-webusers-change-password
                      ring.util.response/response
                      cw/error-response-json))

            ;; TRANSIT
            (POST "/change-password/transit" request
                  (-> request
                      rest-webusers-change-password
                      transit/response-transit))


            )))

;; END Users reference book
;;..................................................................................................
