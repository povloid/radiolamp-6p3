(ns ix.omut.component.thumbinal-modal-edit-form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.thumbinal-edit-form :as thumbnails-edit-form]
            [ix.omut.component.modal-edit-form-for-id--yes-no :as modal-edit-form-for-id--yes-no]))




(def app-init
  (merge modal-edit-form-for-id--yes-no/app-init
         thumbnails-edit-form/app-init))

(defn component [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--yes-no/component app
                {:opts (assoc opts :edit-form-for-id thumbnails-edit-form/component)}))))
