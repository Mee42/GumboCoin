package com.gumbocoin.server

import spark.Spark.*


fun initServerHttpsInterface(){
    println("/api/${inputArguments.release.str}")


    
    path("/api/${inputArguments.release.str}"){
        get("/hello"){_,_ -> println("hello");"Hello, World!" }
    }

}