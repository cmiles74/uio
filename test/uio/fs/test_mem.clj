(ns uio.fs.test-mem
  (:require [uio.fs.mem :as mem]
            [uio.impl :refer :all]
            [midje.sweet :refer :all]))

(facts "Mem works"
  (mem/reset)

  (exists? "mem:///123")      => false
  (exists? "mem:///123/")     => false

  (mkdir "mem:///123")        => nil

  (exists? "mem:///123")      => true
  (exists? "mem:///123/")     => true
  
  (mkdir "mem:///123/456")    => nil
  (mkdir "mem:///123/456/aa") => nil

  (attrs "mem:///")           => {:url "mem:///"     :dir true}
  (attrs "mem:///123")        => {:url "mem:///123/" :dir true}
  (attrs "mem:///123/")       => {:url "mem:///123/" :dir true}

  ; put some files
  (spit (to "mem:///123/456.txt")       "a")    => nil
  (spit (to "mem:///123/456/aa.txt")    "bb")   => nil
  (spit (to "mem:///123/456/aa/bb.txt") "ccc")  => nil
  (spit (to "mem:///123/456/cc.txt")    "dddd") => nil

  (ls "mem:///123/456.txt")   => [{:url "mem:///123/456.txt" :size 1}]
  (ls "mem:///123/456.txt/")  => (throws #"There's something that is not a directory: \"mem:///123/456.txt\"")

  ; try creating directories over existing dir/files and reading/writing files to a directory
  (mkdir "mem:///123")            => (throws #"Directory already exists")
  (attrs "mem:///doesn't/exist")  => (throws #"File not found")
  (mkdir "mem:///123/456.txt")    => (throws #"File already exists")
  (from  "mem:///123/")           => (throws #"Directory already exists")
  (spit (to "mem:///123/") "123") => (throws #"Directory already exists")

  ; ls
  (ls "mem:///")        => [{:url "mem:///123/"              :dir  true}]

  (ls "mem:///123")     => (ls "mem:///123/")
  (ls "mem:///123")     => [{:url "mem:///123/456.txt"       :size 1}
                            {:url "mem:///123/456/"          :dir  true}]
  
  (ls "mem:///123/456") => [{:url "mem:///123/456/aa.txt"    :size 2}
                            {:url "mem:///123/456/aa/"       :dir  true}
                            {:url "mem:///123/456/cc.txt"    :size 4}]
  
  (ls "mem:///"
      {:recurse true})  => [{:url "mem:///123/"              :dir  true}
                            {:url "mem:///123/456.txt"       :size 1}
                            {:url "mem:///123/456/"          :dir  true}
                            {:url "mem:///123/456/aa.txt"    :size 2}
                            {:url "mem:///123/456/aa/"       :dir  true}
                            {:url "mem:///123/456/aa/bb.txt" :size 3}
                            {:url "mem:///123/456/cc.txt"    :size 4}]

  ; assert delete works
  (attrs "mem:///123/456/aa") => {:url "mem:///123/456/aa/" :dir true}
  (empty? (ls "mem:///123/456/aa")) => false
  
  (delete "mem:///123/456/aa") => (throws #"Directory is not empty") ; attempt to delete a non-empty directory
  (delete "mem:///123/456/aa/bb.txt")
  (delete "mem:///123/456/aa")                              ; delete a dir without trailing slash

  (delete "mem:///123/456/cc.txt/") => (throws #"There's something that is not a directory") ; attempt to delete a directory when it's a file

  (delete "mem:///123/456/aa.txt")                          ; delete files
  (delete "mem:///123/456/cc.txt")

  (delete "mem:///123/456/")                                ; delete a dir with a trailing slash

  (ls "mem:///"
      {:recurse true})  => [{:url "mem:///123/"        :dir true}
                            {:url "mem:///123/456.txt" :size 1}]

  ; check that files are returned in ascending order
  (mem/reset)

  (mkdir "mem:///file-2")

  (spit (to (str "mem:///file-3.txt"))   "hello")
  (spit (to (str "mem:///file-2.txt"))   "hello")
  (spit (to (str "mem:///file-2/2.txt")) "hello")
  (spit (to (str "mem:///file-1.txt"))   "hello")

  (->> (ls "mem:///")
       (map :url)) => (->> (ls "mem:///")
                           (map :url)
                           sort)

  (->> (ls "mem:///" {:recurse true})
       (map :url)) => (->> (ls "mem:///" {:recurse true})
                           (map :url)
                           sort)

  (mem/reset))
