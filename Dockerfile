FROM eclipse-temurin:21-jre-jammy

ENV NODE_VERSION=16.13.0
RUN apt update
RUN apt install -y curl
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
ENV NVM_DIR=/root/.nvm
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"

COPY target/*.jar /app/app.jar

EXPOSE 8080

WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]

LABEL org.opencontainers.image.source=https://github.com/moguyn/deepdesk
LABEL org.opencontainers.image.description="Deepdesk is a desktop application for AI agents"
LABEL org.opencontainers.image.url=https://chat.ycargo.cn
LABEL org.opencontainers.image.documentation=https://chat.ycargo.cn/docs
LABEL org.opencontainers.image.vendor="Moguyn"
