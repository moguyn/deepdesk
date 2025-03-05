FROM scratch

COPY ./deepdesk /

EXPOSE 8080

ENTRYPOINT ["/deepdesk"]

LABEL org.opencontainers.image.source=https://github.com/moguyn/deepdesk
LABEL org.opencontainers.image.description="Deepdesk is a desktop application for AI agents"
LABEL org.opencontainers.image.url=https://chat.ycargo.cn
LABEL org.opencontainers.image.documentation=https://chat.ycargo.cn/docs
LABEL org.opencontainers.image.vendor="Moguyn"
