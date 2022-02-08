const epub = require('epub-gen');
const express = require('express')
const fs = require("fs");


const app = express()
app.use(express.json({limit: '50mb'}));


let postedContent = []

app.post('/add',(req,res)=>{ //POST request submitted at root page
    console.log("request received at add of root")
    console.log(req.body)
    postedContent.push(req.body)
    res.send("data received")
})

app.post('/make',(req,res)=>{ //POST request submitted at root page
    console.log("request received at make of root")
    console.log(req.body)
    makepub(req.body)
    res.send("data received")
})


// read app.css file

const cssContent = fs.readFileSync("./app.css");

app.listen(3000)
console.log("server started at port 3000")


function makepub(body){
// create epub from the content
    const options = {
        title: body.title,
        author: body.author,
        css: cssContent,
        cover : body.cover,
        output: './store/'+body.title+'.epub',
        content: postedContent
    };
    new epub(options).promise.then(() =>{
        // empty postedContent
        postedContent = []
        console.log('Done')
    } );
}