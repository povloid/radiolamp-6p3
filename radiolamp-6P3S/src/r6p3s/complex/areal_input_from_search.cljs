(ns r6p3s.complex.areal-input-from-search
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require ;;[cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.cpt.input-from-search :as input-from-search]
            [r6p3s.complex.areal-search-view :as areal-search-view]))




(def app-init
  (input-from-search/app-init areal-search-view/app-init))

(def get-selected input-from-search/get-selected)

(def component
  (input-from-search/component
   areal-search-view/component
   {:label-one                 "Географическая область"
    :label-multi               "Географические области"
    :placeholder               "Выберите область..."
    :ui-type--add-button--type :primary
    :ui-type--add-button--text "Добавть область..."
    :multiselect-row-render-fn (fn [app-row]
                                 (om/build areal-search-view/td-view app-row))
    :one--row-to-text-fn       :path_keynames
    }))
