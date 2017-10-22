(ns reagent-query.core
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn keyword-to-map [kw]
  (let [[s attr] (str/split (name kw) #"[:]")
        [elem & classes] (str/split s #"[.]")]
    {:elem (if (= elem "") nil (keyword elem))
     :classes (set classes)
     :attr attr}))

(defn query-step [vec step]
  (let [step (cond (keyword? step) (keyword-to-map step)
                   (map? step) step
                   (vector? step) (let [[kw attr-vals] step]
                                    (-> (keyword-to-map kw)
                                        (assoc :attr-vals attr-vals))))]
    (cond (seq? vec)
          (mapcat #(query-step % step) vec)
          :else
          (let [[act-elem & content] vec
                [attrs content] (cond (map? (first content))
                                      [(first content) (rest content)]
                                      :else
                                      [{} content])
                act-classes (set (str/split (:class attrs) #" "))
                {:keys [elem attr classes attr-vals]} step
                [act-elem act-classes] (cond (keyword? act-elem)
                                             (let [[e & c] (str/split (name act-elem) #"[.]")]
                                               [(keyword e) (set/union (set c) act-classes)])
                                             :else
                                             [act-elem act-classes])]
            (cond
              (or (nil? elem)
                  (= elem act-elem))
              (cond (every? #(contains? act-classes %) classes)
                    (cond (nil? attr)
                          (cond (or (nil? attr-vals)
                                    (every? (fn [[k v]]
                                              (= (attrs k) v)) attr-vals))
                                content
                                :else
                                (list))
                          :else
                          (list (attrs (keyword attr))))
                    :else
                    (list))
              :else
              (list))))))

(defn query [vec & path]
  (loop [res (list vec)
         path path]
    (cond
      (empty? path)
      res
      :else
      (recur (mapcat #(query-step % (first path)) res) (rest path)))))

(defn mock-change-event [val]
  (let [ctrl (js-obj)
        ev (js-obj)]
    (aset ctrl "value" val)
    (aset ev "target" ctrl)
    ev))

(defn all-elems [root]
  (cond (vector? root) (cons root (->> (rest root)
                                       (mapcat all-elems)))
        (seq? root)    (mapcat all-elems root)
        :else          (list)))

(defn find [root & path]
  (apply query (all-elems root) path))
