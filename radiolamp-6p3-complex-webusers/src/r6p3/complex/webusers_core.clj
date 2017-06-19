(ns r6p3.complex.webusers-core
  (:use r6p3.core)
  (:require [korma.db :as kdb]
            [korma.core :as kc]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))


;;**************************************************************************************************
;;* BEGIN entity webuser
;;* tag: <entity webuser>
;;*
;;* description: пользователь системы
;;*
;;**************************************************************************************************

(kc/defentity webuser
  (kc/pk :id)
  (kc/transform (fn [row] (dissoc row :fts :password))))

(kc/defentity webuser!!!
  (kc/table :webuser)
  (kc/pk :id)
  (kc/transform (fn [row] (dissoc row :fts))))


(defn webuser-save
  "Сохранить пользователя"
  [webuser-row]
  (com-save-for-id webuser webuser-row))

(defn webuser-save-for-username
  "Сохранить пользователя по имени"
  [webuser-row]
  (com-save-for-field webuser :username webuser-row))

(def webuser-select* (kc/select* webuser))

(defn webuser-find-by-username [username]
  (-> webuser-select*
      (kc/where (= :username username))
      com-exec-1))

(defn webuser-find-by-username!!! [username]
  (-> webuser!!!
      kc/select*
      (kc/where (= :username username))
      com-exec-1))


(defn webuser-pred-search? [select*-1 fts-query]
  (com-pred-full-text-search* select*-1 :fts fts-query))


(defn webuser-get-username-from-request [request]
  (-> request
      :session
      :cemerick.friend/identity
      :current))


(defn webuser-get-from-request [request]
  (-> request
      webuser-get-username-from-request
      webuser-find-by-username))

;; END entity webuser
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN entity webrole
;;* tag: <entity webrole>
;;*
;;* description:роли пользователей системы
;;*
;;**************************************************************************************************


(kc/defentity webrolesgroup
  (kc/pk :id)
  (kc/prepare (partial prepare-as-string :keyname))
  (kc/transform (partial transform-as-keyword :keyname)))


(defn webrolesgroup-init [row]
  (com-save-for-field webrolesgroup :keyname (update-in row [:keyname] name)))


(kc/defentity webrole
  (kc/pk :id)
  (kc/belongs-to webrolesgroup)
  (kc/prepare (partial prepare-as-string :keyname))
  (kc/transform (partial transform-as-keyword :keyname)))

(defn webrole-init [row]
  (com-save-for-field webrole :keyname (update-in row [:keyname] name)))

(defn webrole-list
  ([] (webrole-list false))
  ([with-webrolesgroup]
   (if with-webrolesgroup
     (kc/select webrole (kc/with webrolesgroup))
     (kc/select webrole))))


;; END entity webrole
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN entity webuserwebrole
;;* tag: <entity webuserwebrole>
;;*
;;* description: связь пользователей и ролей
;;*
;;**************************************************************************************************

(kc/defentity webuserwebrole
  (kc/pk :id))

(defn webuserwebrole-add-rel [webuser-row webrole-row]
  ((com-defn-add-rel-many-to-many :id :id webuserwebrole :webuser_id :webrole_id) webuser-row webrole-row))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels [{id :id :as webuser-row} webrole-rows]
  (kdb/transaction
   (kc/delete webuserwebrole (kc/where (= :webuser_id id)))
   (doall (map (partial webuserwebrole-add-rel webuser-row) webrole-rows))))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels-for-keynames [webuser-row webrole-rows-keynames]
  (->> webrole-rows-keynames
       (map #(first (kc/select webrole (kc/where (= :keyname (name %))))))
       (webuserwebrole-add-rels webuser-row)
       kdb/transaction))

(defn webuserwebrole-webuser-has-a-role? [webuser-row webrole-keyname]
  ((com-defn-has-a-rel? webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row webrole-keyname))

(defn webuserwebrole-own-get-rels-set [webuser-row]
  ((com-defn-get-rels-set webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row))

(defn webuserwebrole-get-rels-set-for-username [username]
  (-> username
      webuser-find-by-username
      webuserwebrole-own-get-rels-set
      kdb/transaction))

(defn webuserwebrole-get-rels-set-from-request [request]
  (-> request
      :session
      :cemerick.friend/identity
      :current
      webuserwebrole-get-rels-set-for-username ))


(defn contains-webroles? [roles-keys-set roles]
  (reduce #(or % (contains? roles-keys-set (:keyname %2))) false roles))


(defn keys-for-roles [row roles-keys-set roles ks]
  (if (contains-webroles? roles-keys-set roles)
    row (reduce dissoc row ks)))



(defn is-allow-from-request [request roles]
  (-> request
      webuserwebrole-get-rels-set-from-request
      (contains-webroles? roles)))


(defn throw-when-no-role [roles-keys-set role]
  (when (not (contains? roles-keys-set (:keyname role)))
    (throw (Exception. "У вас нет прав доступа для данной функции!"))))

(defn throw-when-no-roles [roles-keys-set roles]
  (when (not (contains-webroles? roles-keys-set roles))
    (throw (Exception. "У вас нет прав доступа для данной функции!"))))

(defn throw-when-no-role-from-request [request webrole-row]
  (throw-when-no-role
      (webuserwebrole-get-rels-set-from-request request)
    webrole-row))

;; дополнительный функционал
(defn add-user [username password roles-keys-set]
  (let [u (webuser-save {:username username
                         :password (cemerick.friend.credentials/hash-bcrypt password)})]
    (webuserwebrole-add-rels-for-keynames u roles-keys-set)))

;; END entity webuserwebrole
;;..................................................................................................
