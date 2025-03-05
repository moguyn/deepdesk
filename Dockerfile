FROM scratch

COPY target/deepdesk-linux /deepdesk

RUN chmod +x /deepdesk

EXPOSE 8080

ENTRYPOINT ["/deepdesk"]
