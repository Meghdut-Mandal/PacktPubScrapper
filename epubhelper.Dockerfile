
# node js 16 docker image
FROM node:16-alpine
WORKDIR /usr/app
COPY epubhelper/package.json .
RUN npm install
COPY epubhelper/src/  .
RUN ls -la
CMD ["node", "main.js"]


