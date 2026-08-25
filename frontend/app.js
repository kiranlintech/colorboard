const API="/api/tasks";

async function loadTasks(){
  const res=await fetch(API);
  const tasks=await res.json();
  document.getElementById("total").textContent=tasks.length;
  document.getElementById("completed").textContent=tasks.filter(t=>t.status==="COMPLETED").length;
  document.getElementById("pending").textContent=tasks.filter(t=>t.status==="PENDING").length;
  document.getElementById("progress").textContent=tasks.filter(t=>t.status==="IN_PROGRESS").length;

  const el=document.getElementById("tasks");
  el.innerHTML=tasks.map(t=>`
    <article class="task ${t.color}">
      <h3>${escapeHtml(t.title)}</h3>
      <p>${escapeHtml(t.description||"No description")}</p>
      <span class="status">${t.status}</span>
      <div class="actions">
        ${getActionButtons(t)}
        <button class="delete" onclick="deleteTask(${t.id})">Delete</button>
      </div>
    </article>`).join("");
}

function getActionButtons(task){
  if(task.status==="PENDING"){
    return `<button onclick="updateStatus(${task.id}, 'IN_PROGRESS')">→ In Progress</button>`;
  }else if(task.status==="IN_PROGRESS"){
    return `<button onclick="updateStatus(${task.id}, 'COMPLETED')">✓ Complete</button>`;
  }else if(task.status==="COMPLETED"){
    return `<button disabled>✓ Completed</button>`;
  }
  return "";
}

async function updateStatus(id, newStatus){
  await fetch(`${API}/${id}`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({status:newStatus})});
  loadTasks();
}

async function addTask(e){
  e.preventDefault();
  await fetch(API,{method:"POST",headers:{"Content-Type":"application/json"},
    body:JSON.stringify({
      title:document.getElementById("title").value,
      description:document.getElementById("description").value,
      status:document.getElementById("status").value,
      color:document.getElementById("color").value
    })});
  e.target.reset(); loadTasks();
}

async function completeTask(id){
  await fetch(`${API}/${id}`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({status:"COMPLETED"})});
  loadTasks();
}

async function deleteTask(id){if(confirm("Delete this task?")){await fetch(`${API}/${id}`,{method:"DELETE"});loadTasks();}}

function escapeHtml(v){return String(v).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]));}

document.getElementById("taskForm").addEventListener("submit",addTask);
loadTasks();
