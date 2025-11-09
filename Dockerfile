# Use an official JDK image
FROM openjdk:17-jdk-slim

# Create and switch to app directory
WORKDIR /app

# Copy project files
COPY . .

# Compile the Java files
RUN javac -cp "lib/*" src/*.java -d bin

# Expose the port (Render sets $PORT automatically)
ENV PORT=8080
EXPOSE 8080

# Run the app
CMD ["sh", "-c", "java -cp 'bin:lib/*' EduSmartServer"]
