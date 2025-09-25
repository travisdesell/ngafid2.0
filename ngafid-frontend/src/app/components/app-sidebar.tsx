import { Calendar, Home, Inbox, Search, Settings, UserPlus } from "lucide-react"

import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarRail,
    SidebarTrigger,
} from "@/components/ui/sidebar"

// Menu items.
const items = [
    {
        title: "Home",
        url: "#",
        icon: Home,
    },
    {
        title: "Inbox",
        url: "#",
        icon: Inbox,
    },
    {
        title: "Calendar",
        url: "#",
        icon: Calendar,
    },
    {
        title: "Search",
        url: "#",
        icon: Search,
    },
    {
        title: "Settings",
        url: "#",
        icon: Settings,
    },
];

const itemsLogin = [
    {
        title: "Login",
        url: "#",
        icon: Home,
    },
    {
        title: "Register",
        url: "#",
        icon: UserPlus,
    },
]

const itemsFooter = [
    {
        title: "Settings",
        url: "#",
        icon: Settings,
    },
]


export function AppSidebar() {
  return (
    <Sidebar collapsible="icon">
        <SidebarHeader>
            <SidebarMenu>
                Test
            </SidebarMenu>
        </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {items.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton asChild>
                    <a href={item.url}>
                      {/* <item.icon size={4} /> */}
                      <span>{item.title}</span>
                    </a>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  )
}

// export function AppSidebar() {
//     return (
//         <Sidebar collapsible="icon">

//             {/* Main Sidebar Area */}
//             <SidebarContent>
//                 <SidebarGroup>

//                     {/* Header */}
//                     <SidebarGroupLabel>
//                         NGAFID
//                     </SidebarGroupLabel>

//                     {/* Main Content */}
//                     <SidebarGroupContent>
//                         <SidebarMenu>
//                             {itemsLogin.map((item) => (
//                                 <SidebarMenuItem key={item.title}>
//                                     <SidebarMenuButton asChild>
//                                         <a href={item.url}>
//                                             <item.icon />
//                                             <span>{item.title}</span>
//                                         </a>
//                                     </SidebarMenuButton>
//                                 </SidebarMenuItem>
//                             ))}
//                         </SidebarMenu>
//                     </SidebarGroupContent>

//                 </SidebarGroup>
//             </SidebarContent>

//             {/* Footer Content */}
//             <SidebarFooter>
//                 <SidebarMenu>
//                     {itemsFooter.map((item) => (
//                         <SidebarMenuItem key={item.title}>
//                             <SidebarMenuButton asChild>
//                                 <a href={item.url}>
//                                     <item.icon />
//                                     <span>{item.title}</span>
//                                 </a>
//                             </SidebarMenuButton>
//                         </SidebarMenuItem>
//                     ))}
//                 </SidebarMenu>
//             </SidebarFooter>
//         </Sidebar>
//     )
// }