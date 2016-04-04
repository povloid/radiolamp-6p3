(ns ix.omut.component.table-1
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.ui.panel :as panel]
            [ix.omut.ui.table :as table]
            [ix.omut.ui.glyphicon :as gicon]
            [ix.omut.ui.media :as media]))


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
