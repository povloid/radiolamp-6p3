(ns r6p3s.cpt.table-1
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.panel :as panel]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.glyphicon :as gicon]
            [r6p3s.ui.media :as media]))


                                        ;TODO: НАдо переименовать этот компонент !!!!!

(def app-init  [])


(defn component [app own]
  (reify
    om/IRender
    (render [_]
      (if (empty? @app)
        (dom/h2 nil "Нет файлов")
        (panel/render
         {:heading           " Список файлов"
          :badge             (str (count @app))
          :heading-glyphicon "folder-open"
          :type              :primary
          :after-body
          (table/render
           {:hover?      true
            :bordered?   true
            :striped?    true
            :responsive? true
            :tbody
            (->> @app
                 (map (fn [{:keys [filename path top_description description size]}]
                        (dom/tr nil
                                (dom/td nil
                                        (media/render {:href         path
                                                       :media-object (gicon/render "file" nil "5em")
                                                       :heading      filename
                                                       :heading-2    top_description
                                                       :body         (dom/p nil description)})))))
                 (apply dom/tbody nil))})})))))
