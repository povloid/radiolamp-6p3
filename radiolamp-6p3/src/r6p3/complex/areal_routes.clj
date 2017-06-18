;;;**************************************************************************************************
;;;* BEGIN Web layer for areal
;;;* tag: <routes areal>
;;;*
;;;* description: Маршруты для Области, районы...
;;;*
;;;**************************************************************************************************

(ns r6p3.complex.areal-routes
  (:use compojure.core)
  (:use r6p3.transit)
  (:require [r6p3.transit :as transit]
            [r6p3.complex.areal-web :as wb]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))


(defn make-routes* [{:keys [uri] :as spec}]
  (routes
   (context (str uri "/areal") []

            ;; TRANSIT
            (POST "/list" request
                  (-> request
                      (wb/rest-areal-list spec)
                      transit/response-transit))
            ;; TRANSIT
            (POST "/find" request
                  (-> request
                      (wb/rest-areal-find spec)
                      transit/response-transit))
            ;; TRANSIT
            (POST "/save" request
                  (-> request
                      (wb/rest-areal-save spec)
                      transit/response-transit))
            ;; TRANSIT
            (POST "/delete" request
                  (-> request
                      (wb/rest-areal-delete spec)
                      transit/response-transit)))))
