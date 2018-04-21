# Launcher for Micro Service Libraries

## Purpose

Because I don't want to deliver "Uber.jar" and "Big Container Image", So I need a launcher for sepalated jar file.

## 1. Kumuluz EE Launcher

* Usage

1. Create pom.xml and add dependency for JavaEE Project

    NOTICE: Cannot add dependency for multiple JAX-RS Applications!!
    
    ```xml
        <!-- type:jar -->
        <dependency>
            <groupId>com.hogegroup</groupId>
            <artifactId>hogeapp</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
    ```
   or
    ```xml
        <!-- type:war -->
        <dependency>
            <groupId>com.hogegroup</groupId>
            <artifactId>hogewebapp</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <classifier>classes<classifier>
        </dependency>
    ```

1. Add dependency for com.chromosphere:microservice.launcher:1.0.0

    ```xml
        <dependency>
            <groupId>com.chromosphere</groupId>
            <artifactId>microservice.launcher</artifactId>
            <version>1.0.0</version>
        </dependency>
    ```

1. execute following command

    ```shell
    PORT={nnnn} mvn clean exec:java com.chromosphere.micrroservice.launcher.KumuluzeeLauncher
    ```
