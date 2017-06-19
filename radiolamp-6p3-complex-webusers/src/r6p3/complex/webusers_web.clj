;;**************************************************************************************************
;;* BEGIN Web layer for webusers
;;* tag: <ref book web layer webusers>
;;*
;;* description: Вебслой для работы с пользователями
;;*
;;**************************************************************************************************

(ns r6p3.complex.webusers-web
  (:require [clojure.java.io :as io]
            [r6p3.core :as c]
            [r6p3.complex.webusers-core :as cc]
            [r6p3.transit :as transit]
            [r6p3.core :as c]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))



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

  (if (cc/is-allow-from-request request #{edit-role view-role})
    (-> cc/webuser-select*
        (c/com-pred-page* (dec page) page-size)

        (as-> query
            (let [fts-query (clojure.string/trim fts-query)]
              (if (empty? fts-query)
                query
                (cc/webuser-pred-search? query fts-query))))


        (as-> query
            (if webusers-list-pred-fn
              (webusers-list-pred-fn query request)
              query))

        (korma.core/order :id :desc)
        c/com-exec)
    []))

(defn rest-webusers-find [{{id :id} :params :as request} {:keys [view-role]}]

  (cc/throw-when-no-role-from-request request view-role)

  (let [webroles (cc/webrole-list true)]
    (-> (if id
          (-> id
              ((partial c/com-find cc/webuser))
              (dissoc :password)
              (as-> row
                  (assoc row :troles-set
                         [webroles (cc/webuserwebrole-own-get-rels-set row)])))
          (-> {} ;; new
              (assoc :troles-set
                     [webroles #{}]))))))

(defn rest-webusers-save [request {:keys [edit-role]}]

  (cc/throw-when-no-role-from-request request edit-role)

  (let [{:keys [row user-roles-keys-set]} (request :params)
        row (-> row
                (as-> row
                    (if (empty? (:password row)) (dissoc row :password)
                        (update-in row [:password] creds/hash-bcrypt)))
                ((partial c/com-save-for-id cc/webuser)))]

    (when user-roles-keys-set
      (println (map keyword user-roles-keys-set))
      (cc/webuserwebrole-add-rels-for-keynames row (map keyword user-roles-keys-set)))
    row))

(defn rest-webusers-delete [request {:keys [edit-role]}]

  (cc/throw-when-no-role-from-request request edit-role)

  (-> request
      :params
      :id
      ((partial c/com-delete-for-id cc/webuser))
      ((fn [_] {:result "OK"}))))

(defn rest-webusers-change-password [request]
  (-> request
      :params
      (update-in [:password] creds/hash-bcrypt)
      (assoc :username (-> request :session :cemerick.friend/identity :current))
      cc/webuser-save-for-username))
