FROM scratch

COPY ./deepdesk /

EXPOSE 8080

ENTRYPOINT ["/deepdesk"]
