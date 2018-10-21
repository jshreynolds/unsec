(ns unsec.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as stringo]
            [clojure.java.io :as io]))

(def ^:dynamic *base-url* "http://www.un.org")

(defn get-fqdn-url
  [fragment]
  (str *base-url* fragment))

(defn get-repertoire-url [lang]
  (get-fqdn-url (str "/" lang "/sc/repertoire/")))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn get-menu-href [menu-a-tag]
  ((menu-a-tag :attrs) :href))

(defn menu-items [lang]
  (html/select (fetch-url (get-repertoire-url lang)) [:div#mainnav :a]))

(defn menu-page-links [lang]
  (let
   [drop-anchor #(stringo/replace %1 #"#.*" "")
    drop-root #(stringo/replace %1 *base-url* "")
    items (menu-items lang)]
    (distinct (map #(-> %1 get-menu-href drop-anchor drop-root) items))))

(defn menu-entry [menu-node]
  (str "\"" (first (menu-node :content)) "\"," (get-menu-href menu-node)))

(defn menu-toc [lang]
  (map
   menu-entry
   (menu-items lang)))

(defn page-content [url]
  (html/select (fetch-url url) [:div#content]))

(defn html-page-content [url]
  (apply str (html/emit* (page-content url))))

(defn write-page-to-file
  [fragment-url
   base-dir]
  (let [full-url (get-fqdn-url fragment-url)
        file-content (html-page-content full-url)
        file-path (str base-dir fragment-url)]
    (io/make-parents file-path)
    (spit file-path file-content :append true)))

(defn save-repertoire-contents [lang]
  (let [toc (menu-toc lang)
        pages (menu-page-links lang)
        base-dir "./output"
        output-csv (str base-dir "/" lang "/" lang "-menu.csv")]
    (doseq [page pages]
      (println (str "writing " page " ..."))
      (write-page-to-file page base-dir))
    (with-open [wrtr (io/writer output-csv)]
      (doseq [line toc]
        (.write wrtr (str line "\n"))))))


(def languages ["ar" "zh" "en" "fr" "ru" "es"])
;(def languages ["en"])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (doseq [lang languages]
    (save-repertoire-contents lang)))

