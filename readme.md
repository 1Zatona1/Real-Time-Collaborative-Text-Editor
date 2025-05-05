## How to run:

 1. Clone the repository or download the zip file using:
    ``` sh
    git clone https://github.com/1Zatona1/Real-Time-Collaborative-Text-Editor.git
    ```
   
2. Redirect to the server folder:
    ``` sh
   cd Real-Time-Collaborative-Text-Editor/server
   ```

3. Make sure that have the correct java version

4. Build the springboot server using
    ``` sh
   mvn clean install
    ```

5. Run the server using
    ``` sh
   mvn spring-boot:run
    ```

6. In another terminal navigate to the project root
    ``` sh
   cd Real-Time-Collaborative-Text-Editor
    ```

7.  Build the project
    ``` sh
    mvn clean package
    ```

8. Run the application
    ``` sh
    mvn exec:java -Dexec.mainClass="com.example.apt_project.HelloApplication"
    ```