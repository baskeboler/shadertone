(ns shadertone.api
  (:require [clj-http.client :as http]
            [cheshire.core :refer :all]))

(defn create-shadertoy-key-file []
  (print "Enter shadertoy api key: ")
  (let [k  (read-line)]
    (spit ".shadertoy.key" k)
    k))

(-> (new java.io.File ".shadertoy.key")
    (.delete ))

(def st-api-key
  (try
    (-> (slurp ".shadertoy.key")
        (clojure.string/trim-newline))
    (catch java.io.FileNotFoundException e
      (println "Resolving api key")
      (create-shadertoy-key-file))))
  (def shadertoy-base "https://www.shadertoy.com")
(def shadertoy-api-base-url "https://www.shadertoy.com/api/v1/shaders")
(defn search-shader [q]
  (http/get (str shadertoy-api-base-url "/query/" q ) {:query-params {:key st-api-key}
                                                       :as :json}))
(defn get-shader [id]
  (http/get (str shadertoy-api-base-url "/" id)
            {:query-params {:key st-api-key}
             :as :json}))
(def batman-shaders
  (-> (search-shader "batman")
      (get-in [:body :Results])))
(def water (-> (search-shader "ocean") (get-in [:body :Results])))
(def shader-main
  "
  void main(void) {
    mainImage(gl_FragColor, gl_FragCoord.xy);
  }")
#_(
   (get-shader (first water))
   (save-shader "4tjGRh" "ocean.glsl")
   (save-shader "ldXSDB" "batman.glsl")
   (save-shader "Mld3Rn" "voro.glsl")
   (save-shader "XlSSzK" "sun.glsl")
   (shader-inputs (get-shader "Msl3Rr"))
   (save-shader "4tdSWr" "clouds.glsl"))
(defn shader-inputs [resp]
  (-> resp
      (get-in [:body :Shader :renderpass])
      (first)
      (:inputs)))

(defn- shadertoy-dir []
  (str (System/getProperty "user.home") "/.shadertoy"))
(defn- shaders-dir []
  (str (shadertoy-dir) "/shaders"))
(shaders-dir)

(defn save-shader-spec [id json]
  (let [p (str (shaders-dir) "/" id)
        spec-path  (str (shaders-dir) "/" id "/spec.json")
        rs (get-in json [:Shader :renderpass])]
    (clojure.java.io/make-parents spec-path)
    (spit spec-path (generate-string json))
    (doseq [[idx s] (zipmap (range)   rs) ]
      (spit (str p "/" (:name s) ".glsl") (:code s))
      (doseq [[idx2 input] (zipmap (range) (:inputs s))]
        (save-asset id
                    (last (clojure.string/split
                           (:src input) #"/"))
                    (:body
                     (http/get
                      (str shadertoy-base (:src input)) {:as :stream}))
                    )))))

(defn save-asset [shader-id asset-name s]
  (let [p (str (shaders-dir) "/" shader-id "/assets/" asset-name)]
    (clojure.java.io/make-parents p)
    (clojure.java.io/copy  s (clojure.java.io/file p))))
#_(http/get (str shadertoy-api-base-url "?key=" st-api-key) {:as :json} )

(defn save-shader [id & opts]
  (let [s (get-shader id)
        json (:body s)
        code (-> (get-in s [:body :Shader :renderpass ])
                 (first)
                 (:code)
                 (clojure.string/replace "texture" "texture2D")
                 (clojure.string/replace "iTime" "iGlobalTime")
                 (str shader-main))]
    (save-shader-spec id json)))
