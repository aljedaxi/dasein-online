#!/usr/bin/env bb -i

(defn find-color
  ([v] (find-color v 0))
  ([v i]
   (let [stuff (for [row v :let [x (nth row i)] :when (not (:taken-by x))]
                 x)
         res (first stuff)]
     (or (:hex res) (recur v (+ 1 i))))))

(doall
  (->> *in*
       yaml/parse-stream
       vals
       find-color
       println))
